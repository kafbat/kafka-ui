package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.controller.TopicsController;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.mapper.ClusterMapperImpl;
import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.TopicColumnsToSortDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class TopicsServicePaginationTest {

  private static final String LOCAL_KAFKA_CLUSTER_NAME = "local";

  private final TopicsService topicsService = Mockito.mock(TopicsService.class);
  private final ClustersStorage clustersStorage = Mockito.mock(ClustersStorage.class);
  private final ClusterMapper clusterMapper = new ClusterMapperImpl();
  private final AccessControlService accessControlService = new AccessControlServiceMock().getMock();

  private final TopicsController topicsController =
      new TopicsController(topicsService, mock(TopicAnalysisService.class), clusterMapper);

  private void init(Map<String, InternalTopic> topicsInCache) {

    when(clustersStorage.getClusterByName(isA(String.class)))
        .thenReturn(Optional.of(buildKafkaCluster(LOCAL_KAFKA_CLUSTER_NAME)));
    when(topicsService.getTopicsForPagination(isA(KafkaCluster.class)))
        .thenReturn(Mono.just(new ArrayList<>(topicsInCache.values())));
    when(topicsService.loadTopics(isA(KafkaCluster.class), anyList()))
        .thenAnswer(a -> {
          List<String> lst = a.getArgument(1);
          return Mono.just(lst.stream().map(topicsInCache::get).collect(Collectors.toList()));
        });
    topicsController.setAccessControlService(accessControlService);
    topicsController.setAuditService(mock(AuditService.class));
    topicsController.setClustersStorage(clustersStorage);
  }

  @Test
  public void shouldListFirst25Topics() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, null,
            null, null, null).block();

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
  public void shouldListFirst25TopicsSortedByNameDescendingOrder() {
    var internalTopics = IntStream.rangeClosed(1, 100).boxed()
        .map(Objects::toString)
        .map(name -> new TopicDescription(name, false, List.of()))
        .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
            Metrics.empty(), InternalLogDirStats.empty(), "_"))
        .collect(Collectors.toMap(InternalTopic::getName, Function.identity()));
    init(internalTopics);

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, null,
            TopicColumnsToSortDTO.NAME, SortOrderDTO.DESC, null).block();

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
  public void shouldCalculateCorrectPageCountForNonDivisiblePageSize() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 4, 33, null, null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(1);
    assertThat(topics.getBody().getTopics().get(0).getName()).isEqualTo("99");
  }

  @Test
  public void shouldCorrectlyHandleNonPositivePageNumberAndPageSize() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 0, -1, null, null, null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  public void shouldListBotInternalAndNonInternalTopics() {
    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, Integer.parseInt(name) % 10 == 0, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 0, -1, true, null,
            null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(25);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  public void shouldListOnlyNonInternalTopics() {

    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, Integer.parseInt(name) % 5 == 0, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, 4, -1, false, null,
            null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(4);
    assertThat(topics.getBody().getTopics()).hasSize(5);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  public void shouldListOnlyTopicsContainingOne() {

    init(
        IntStream.rangeClosed(1, 100).boxed()
            .map(Objects::toString)
            .map(name -> new TopicDescription(name, false, List.of()))
            .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), null,
                Metrics.empty(), InternalLogDirStats.empty(), "_"))
            .collect(Collectors.toMap(InternalTopic::getName, Function.identity()))
    );

    var topics = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null, "1",
            null, null, null).block();

    assertThat(topics.getBody().getPageCount()).isEqualTo(1);
    assertThat(topics.getBody().getTopics()).hasSize(20);
    assertThat(topics.getBody().getTopics()).isSortedAccordingTo(Comparator.comparing(TopicDTO::getName));
  }

  @Test
  public void shouldListTopicsOrderedByPartitionsCount() {
    Map<String, InternalTopic> internalTopics = IntStream.rangeClosed(1, 100).boxed()
        .map(i -> new TopicDescription(UUID.randomUUID().toString(), false,
            IntStream.range(0, i)
                .mapToObj(p ->
                    new TopicPartitionInfo(p, null, List.of(), List.of()))
                .collect(Collectors.toList())))
        .map(topicDescription -> InternalTopic.from(topicDescription, List.of(), InternalPartitionsOffsets.empty(),
            Metrics.empty(), InternalLogDirStats.empty(), "_"))
        .collect(Collectors.toMap(InternalTopic::getName, Function.identity()));

    init(internalTopics);

    var topicsSortedAsc = topicsController
        .getTopics(LOCAL_KAFKA_CLUSTER_NAME, null, null, null,
            null, TopicColumnsToSortDTO.TOTAL_PARTITIONS, null, null).block();

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
            null, TopicColumnsToSortDTO.TOTAL_PARTITIONS, SortOrderDTO.DESC, null).block();

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
