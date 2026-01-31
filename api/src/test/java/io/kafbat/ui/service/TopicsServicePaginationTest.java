package io.kafbat.ui.service;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.controller.TopicsController;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.mapper.ClusterMapperImpl;
import io.kafbat.ui.model.CleanupPolicy;
import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.model.TopicColumnsToSortDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class TopicsServicePaginationTest {

  private static final String LOCAL_KAFKA_CLUSTER_NAME = "local";

  private final AdminClientService adminClientService = Mockito.mock(AdminClientService.class);
  private final ReactiveAdminClient reactiveAdminClient = Mockito.mock(ReactiveAdminClient.class);
  private final ClustersStorage clustersStorage = Mockito.mock(ClustersStorage.class);
  private final StatisticsCache statisticsCache = new StatisticsCache(clustersStorage);
  private final ClustersProperties clustersProperties = new ClustersProperties();
  private final TopicsService topicsService = new TopicsService(
      adminClientService,
      statisticsCache,
      clustersProperties
  );

  private final TopicsService mockTopicsService = Mockito.mock(TopicsService.class);
  private final KafkaConnectService kafkaConnectService = Mockito.mock(KafkaConnectService.class);
  private final ClusterMapper clusterMapper = new ClusterMapperImpl();

  private final AccessControlService accessControlService = new AccessControlServiceMock().getMock();

  private final TopicsController topicsController =
      new TopicsController(
          mockTopicsService,
          mock(TopicAnalysisService.class),
          clusterMapper,
          clustersProperties,
          kafkaConnectService
      );

  private void init(Map<String, InternalTopic> topicsInCache) {
    KafkaCluster kafkaCluster = buildKafkaCluster(LOCAL_KAFKA_CLUSTER_NAME);
    statisticsCache.replace(kafkaCluster, Statistics.empty());

    Map<TopicPartition, InternalPartitionsOffsets.Offsets> offsets = topicsInCache.values().stream()
        .flatMap(t ->
            t.getPartitions().values().stream()
                .map(p ->
                        Map.entry(
                            new TopicPartition(t.getName(), p.getPartition()),
                            new InternalPartitionsOffsets.Offsets(p.getOffsetMin(), p.getOffsetMax())
                        )
                )
        ).filter(e ->
            e.getValue().getEarliest() != null && e.getValue().getLatest() != null
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    statisticsCache.update(
        kafkaCluster,
        topicsInCache.entrySet().stream().collect(
            Collectors.toMap(
                Map.Entry::getKey,
                v -> toTopicDescription(v.getValue())
            )
        ),
        topicsInCache.entrySet().stream()
            .map(t ->
                Map.entry(t.getKey(), List.of(new ConfigEntry(CLEANUP_POLICY_CONFIG, "delete")))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
        new InternalPartitionsOffsets(offsets),
        clustersProperties
    );
    when(adminClientService.get(isA(KafkaCluster.class))).thenReturn(Mono.just(reactiveAdminClient));
    when(reactiveAdminClient.listTopics(anyBoolean())).thenReturn(Mono.just(topicsInCache.keySet()));
    when(clustersStorage.getClusterByName(isA(String.class)))
        .thenReturn(Optional.of(kafkaCluster));
    when(mockTopicsService.getTopics(isA(KafkaCluster.class), any(), any(), any()))
        .thenAnswer(a ->
            topicsService.getTopics(
                a.getArgument(0),
                a.getArgument(1),
                a.getArgument(2),
                a.getArgument(3))
        );


    when(mockTopicsService.loadTopics(isA(KafkaCluster.class), anyList()))
        .thenAnswer(a -> {
          List<String> lst = a.getArgument(1);
          return Mono.just(lst.stream().map(topicsInCache::get).collect(Collectors.toList()));
        });

    topicsController.setAccessControlService(accessControlService);
    topicsController.setAuditService(mock(AuditService.class));
    topicsController.setClustersStorage(clustersStorage);
  }

  private TopicDescription toTopicDescription(InternalTopic t) {
    return new TopicDescription(
        t.getName(), t.isInternal(),
        t.getPartitions().values().stream().map(this::toTopicPartitionInfo).toList()
    );
  }

  private TopicPartitionInfo toTopicPartitionInfo(InternalPartition p) {
    return new TopicPartitionInfo(
        p.getPartition(),
        null, List.of(), List.of()
    );
  }


  @Test
  void shouldListFirst25Topics() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
         .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, null,
            null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics())
        .isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  private KafkaCluster buildKafkaCluster(String clusterName) {
    return KafkaCluster.builder()
        .name(clusterName)
        .build();
  }

  @Test
  void shouldListFirst25TopicsSortedByNameDescendingOrder() {
    var internalTopics = IntStream.rangeClosed(1, 100).boxed()
        .map(Objects::toString)
        .map(name -> new TopicDescription(name, false, List.of()))
        .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
            Metrics.empty(), null, null, "_"))
        .collect(Collectors.toMap(InternalTopic::getName, Function.identity()));
    init(internalTopics);

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, null,
            TopicColumnsToSortDTO.NAME, SortOrderDTO.DESC, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName).reversed());
    assertThat(topics.getBody().getTopics()).containsExactlyElementsOf(
        internalTopics.values().stream()
            .map(clusterMapper::toTopic)
            .sorted(Comparator.comparing(TopicDTO::getName).reversed())
            .limit(25)
            .collect(Collectors.toList())
    );
  }

  @Test
  void shouldCalculateCorrectPageCountForNonDivisiblePageSize() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 4, 33, null, null, null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(1);
    assertThat(topics.getBody().getTopics().get(0).getName()).isEqualTo("99");
  }

  @Test
  void shouldCorrectlyHandleNonPositivePageNumberAndPageSize() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 0, -1, null, null, null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  void shouldListBotInternalAndNonInternalTopics() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, Integer.parseInt(name) % 10 == 0, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 0, -1, true, null,
            null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  void shouldListOnlyNonInternalTopics() {

    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, Integer.parseInt(name) % 5 == 0, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 4, -1, false, null,
            null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(5);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  void shouldListOnlyTopicsContainingOne() {

    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), null, null, "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, "1",
            null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(1);
    assertThat(topics.getBody().getTopics()).hasSize(20);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  void shouldListTopicsOrderedByPartitionsCount() {
    Map<String, InternalTopic> internalTopics = IntStream.rangeClosed(1, 100).boxed()
        .map(i -> new TopicDescription(UUID.randomUUID().toString(), false,
            IntStream.range(0, i)
                .mapToObj(p ->
                    new TopicPartitionInfo(p, null, List.of(), List.of()))
                .collect(Collectors.toList())))
        .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), InternalPartitionsOffsets.empty(),
            Metrics.empty(), null, null, "_"))
        .collect(Collectors.toMap(InternalTopic::getName, Function.identity()));

    init(internalTopics);

    var topicsSortedAsc = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null,
            null, TopicColumnsToSortDTO.TOTAL_PARTITIONS, null, null, null).block();

    assertThat(topicsSortedAsc.getBody().getPageCount()).isEqualTo(4);
    assertThat(topicsSortedAsc.getBody().getTopics()).hasSize(25);
    assertThat(topicsSortedAsc.getBody().getTopics()).containsExactlyElementsOf(
        internalTopics.values().stream()
            .map(clusterMapper::toTopic)
            .sorted(Comparator.comparing(TopicDTO::getPartitionCount))
            .limit(25)
            .collect(Collectors.toList())
    );

    var topicsSortedDesc = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null,
            null, TopicColumnsToSortDTO.TOTAL_PARTITIONS, SortOrderDTO.DESC, null, null).block();

    assertThat(topicsSortedDesc.getBody().getPageCount()).isEqualTo(4);
    assertThat(topicsSortedDesc.getBody().getTopics()).hasSize(25);
    assertThat(topicsSortedDesc.getBody().getTopics()).containsExactlyElementsOf(
        internalTopics.values().stream()
            .map(clusterMapper::toTopic)
            .sorted(Comparator.comparing(TopicDTO::getPartitionCount).reversed())
            .limit(25)
            .collect(Collectors.toList())
    );
  }

  @Test
  void shouldListTopicsOrderedByMessagesCount() {
    Map<String, InternalTopic> internalTopics = IntStream.rangeClosed(1, 100).boxed()
        .map(i -> new TopicDescription(UUID.randomUUID().toString(), false,
            IntStream.range(0, i)
                .mapToObj(p ->
                    new TopicPartitionInfo(p, null, List.of(), List.of()))
                .collect(Collectors.toList())))
        .map(topicDescription ->
            InternalTopic.from(topicDescription, List.of(),
                new InternalPartitionsOffsets(
                  topicDescription.partitions().stream()
                      .map(p -> Map.entry(
                          new TopicPartition(topicDescription.name(), p.partition()),
                          new InternalPartitionsOffsets.Offsets(0L, (long) p.partition())
                      )).collect(Collectors.toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue
                      ))
                ),
            Metrics.empty(), null, null, "_")
                .toBuilder().cleanUpPolicy(CleanupPolicy.DELETE).build()
        ).collect(Collectors.toMap(InternalTopic::getName, Function.identity()));

    init(internalTopics);

    var topicsSortedAsc = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null,
            null, TopicColumnsToSortDTO.MESSAGES_COUNT, null, null, null).block();

    assertThat(topicsSortedAsc.getBody().getPageCount()).isEqualTo(4);
    assertThat(topicsSortedAsc.getBody().getTopics()).hasSize(25);
    assertThat(topicsSortedAsc.getBody().getTopics()).containsExactlyElementsOf(
        internalTopics.values().stream()
            .map(clusterMapper::toTopic)
            .sorted(Comparator.comparing(
                (t) -> t.getMessagesCount().get()
            ))
            .limit(25)
            .collect(Collectors.toList())
    );

    var topicsSortedDesc = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null,
            null, TopicColumnsToSortDTO.TOTAL_PARTITIONS, SortOrderDTO.DESC, null, null).block();

    assertThat(topicsSortedDesc.getBody().getPageCount()).isEqualTo(4);
    assertThat(topicsSortedDesc.getBody().getTopics()).hasSize(25);
    assertThat(topicsSortedDesc.getBody().getTopics()).containsExactlyElementsOf(
        internalTopics.values().stream()
            .map(clusterMapper::toTopic)
            .sorted(Comparator.comparing(TopicDTO::getPartitionCount).reversed())
            .limit(25)
            .collect(Collectors.toList())
    );
  }

}
