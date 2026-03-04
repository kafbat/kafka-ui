package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableTable;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.ConsumerGroupLagDTO;
import io.kafbat.ui.model.ConsumerGroupOrderingDTO;
import io.kafbat.ui.model.ConsumerGroupStateDTO;
import io.kafbat.ui.model.InternalTopicConsumerGroup;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

class ConsumerGroupServiceTest {

  @Test
  void getConsumerGroupsForTopicConsumerGroups() {
    // given
    ClustersProperties.Cluster clusterProperties = new ClustersProperties.Cluster();
    clusterProperties.setName("test");

    ClustersProperties clustersProperties = new ClustersProperties();
    clustersProperties.getClusters().add(clusterProperties);


    KafkaCluster cluster = KafkaCluster.builder()
        .name("test")
        .originalProperties(clusterProperties)
        .build();

    ReactiveAdminClient client = Mockito.mock(ReactiveAdminClient.class);
    AdminClientService admin = Mockito.mock(AdminClientService.class);
    Mockito.when(admin.get(cluster)).thenReturn(Mono.just(client));

    final String topic = UUID.randomUUID().toString();
    final String anotherTopic = UUID.randomUUID().toString();

    Map<String, ScrapedClusterState.ConsumerGroupState> consumersWithTopic =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(topic, 0)),
            Map.of(new TopicPartition(topic, 0), 100L),
            ConsumerGroupState.DEAD
        )).limit(10).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> consumersWithoutTopic =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(anotherTopic, 0)),
            Map.of(new TopicPartition(anotherTopic, 0), 100L),
            ConsumerGroupState.DEAD
        )).limit(10).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> stableConsumersWithTopic =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(topic, 0)),
            Map.of(new TopicPartition(topic, 0), 100L),
            ConsumerGroupState.STABLE
        )).limit(10).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> stableConsumersWithoutTopic =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(anotherTopic, 0)),
            Map.of(new TopicPartition(anotherTopic, 0), 100L),
            ConsumerGroupState.STABLE
        )).limit(10).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> consumerGroupStates = new HashMap<>();
    consumerGroupStates.putAll(consumersWithTopic);
    consumerGroupStates.putAll(consumersWithoutTopic);
    consumerGroupStates.putAll(stableConsumersWithTopic);
    consumerGroupStates.putAll(stableConsumersWithoutTopic);

    Mockito.when(client.listConsumerGroups()).thenReturn(Mono.just(
        consumerGroupStates.keySet()
            .stream()
            .map(s -> new ConsumerGroupListing(s, false))
            .toList()
    ));

    Mockito.when(client.listConsumerGroupNames()).thenReturn(Mono.just(
        List.copyOf(consumerGroupStates.keySet())
    ));

    Mockito.when(client.listTopicOffsets(Mockito.eq(topic), Mockito.any(), Mockito.eq(false)))
        .thenReturn(Mono.just(Map.of(new TopicPartition(topic, 0), 100L)));

    Mockito.when(client.describeConsumerGroups(
        Mockito.any())
    ).thenReturn(
        Mono.just(
            consumerGroupStates.values().stream()
                .collect(Collectors.toMap(
                    ScrapedClusterState.ConsumerGroupState::group,
                    ScrapedClusterState.ConsumerGroupState::description
                ))
        )
    );

    Mockito.when(client.listConsumerGroupOffsets(Mockito.any(), Mockito.any())).thenAnswer(
        a -> {
          List<String> groupIds = a.getArgument(0);
          var table = ImmutableTable.<String, TopicPartition, Long>builder();
          for (String groupId : groupIds) {
            ScrapedClusterState.ConsumerGroupState state = consumerGroupStates.get(groupId);
            for (Map.Entry<TopicPartition, Long> entry : state.committedOffsets().entrySet()) {
              table.put(groupId, entry.getKey(), entry.getValue());
            }
          }
          return Mono.just(table.build());
        }
    );

    ScrapedClusterState state = ScrapedClusterState.builder()
        .scrapeFinishedAt(Instant.now())
        .nodesStates(Map.of())
        .topicStates(Map.of())
        .consumerGroupsStates(consumerGroupStates)
        .build();

    Statistics statistics = Statistics.builder()
        .status(ServerStatusDTO.ONLINE)
        .version("Unknown")
        .features(List.of())
        .clusterDescription(ReactiveAdminClient.ClusterDescription.empty())
        .metrics(Metrics.empty())
        .clusterState(state)
        .build();

    StatisticsCache cache = Mockito.mock(StatisticsCache.class);
    Mockito.when(cache.get(cluster)).thenReturn(statistics);

    AccessControlService acl = Mockito.mock(AccessControlService.class);
    ConsumerGroupService consumerGroupService =
        new ConsumerGroupService(admin, acl, clustersProperties, cache);

    // should
    List<InternalTopicConsumerGroup> groups =
         consumerGroupService.getConsumerGroupsForTopic(cluster, topic).block();

    assertThat(groups).size().isEqualTo(
        consumersWithTopic.size() + stableConsumersWithTopic.size()
    );

    List<String> resultedGroupIds = groups.stream().map(InternalTopicConsumerGroup::getGroupId).toList();
    assertThat(resultedGroupIds).containsAll(consumersWithTopic.keySet());

    assertThat(resultedGroupIds).containsAll(stableConsumersWithTopic.keySet());
  }

  private ScrapedClusterState.ConsumerGroupState generate(
      List<TopicPartition> topicPartitions,
      Map<TopicPartition, Long> lastOffsets,
      ConsumerGroupState state
  ) {
    return generate(topicPartitions, lastOffsets, state, 1);
  }


  private ScrapedClusterState.ConsumerGroupState generate(
      List<TopicPartition> topicPartitions,
      Map<TopicPartition, Long> lastOffsets,
      ConsumerGroupState state,
      long lagPerPartition
  ) {
    final String name = UUID.randomUUID().toString();
    Map<TopicPartition, Long> commited = topicPartitions.stream()
        .map(tp -> Map.entry(tp, lastOffsets.get(tp) - lagPerPartition))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));

    List<MemberDescription> members = state.equals(ConsumerGroupState.STABLE) ? List.of(
        new MemberDescription(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            "localhost",
            new MemberAssignment(new HashSet<>(topicPartitions))
        )
    ) : List.of();

    return new ScrapedClusterState.ConsumerGroupState(
        name,
        new ConsumerGroupDescription(
            name,
            false,
            members, "",
            state,
            null
        ), commited
    );
  }

  @ParameterizedTest
  @MethodSource("consumerGroupsLags")
  void calculateConsumerGroupsLags(String topic,
                                   Map<TopicPartition, Long> endOffsets,
                                   long lagPerPartition,
                                   long expectedLag,
                                   Optional<Instant> scapedInstant,
                                   Optional<Instant> queryInstant,
                                   boolean expectedResult) {
    // given
    ClustersProperties.Cluster clusterProperties = new ClustersProperties.Cluster();
    clusterProperties.setName("test");

    ClustersProperties clustersProperties = new ClustersProperties();
    clustersProperties.getClusters().add(clusterProperties);

    AdminClientService admin = Mockito.mock(AdminClientService.class);

    KafkaCluster cluster = KafkaCluster.builder()
        .name("test")
        .originalProperties(clusterProperties)
        .build();

    Map<String, ScrapedClusterState.ConsumerGroupState> consumers =
        Stream.generate(() -> generate(
            List.of(
                new TopicPartition(topic, 0),
                new TopicPartition(topic, 1)
            ),
            endOffsets,
            ConsumerGroupState.DEAD,
            lagPerPartition
        )).limit(10).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    ScrapedClusterState state = ScrapedClusterState.builder()
        .scrapeFinishedAt(scapedInstant.orElse(Instant.now()))
        .nodesStates(Map.of())
        .topicStates(Map.of(
            topic,
            new ScrapedClusterState.TopicState(
                topic, null, List.of(), Map.of(),
                endOffsets.entrySet().stream()
                    .filter(t -> t.getKey().topic().equals(topic))
                    .collect(
                        Collectors.toMap(
                            e -> e.getKey().partition(),
                            Map.Entry::getValue
                        )
                    ), null, null
            )
        ))
        .consumerGroupsStates(consumers)
        .build();

    Statistics statistics = Statistics.builder()
        .status(ServerStatusDTO.ONLINE)
        .version("Unknown")
        .features(List.of())
        .clusterDescription(ReactiveAdminClient.ClusterDescription.empty())
        .metrics(Metrics.empty())
        .clusterState(state)
        .build();

    StatisticsCache cache = Mockito.mock(StatisticsCache.class);
    Mockito.when(cache.get(cluster)).thenReturn(statistics);

    AccessControlService acl = Mockito.mock(AccessControlService.class);
    ConsumerGroupService consumerGroupService =
        new ConsumerGroupService(admin, acl, clustersProperties, cache);

    Tuple2<Map<String, ConsumerGroupLagDTO>, Optional<Long>> result =
        consumerGroupService.getConsumerGroupsLag(
            cluster, consumers.keySet(), queryInstant.map(Instant::toEpochMilli)
        ).block();

    assertThat(result).isNotNull();
    if (expectedResult) {
      assertThat(result.getT1()).hasSize(consumers.size());
      assertThat(result.getT2()).isPresent();
      assertThat(result.getT2()).get().isEqualTo(state.getScrapeFinishedAt().toEpochMilli());

      for (ConsumerGroupLagDTO dto : result.getT1().values()) {
        assertThat(dto.getLag()).isEqualTo(expectedLag);
        assertThat(dto.getTopics()).size().isEqualTo(1);
        assertThat(dto.getTopics().get(topic)).isEqualTo(expectedLag);
      }
    } else {
      assertThat(result.getT1()).isEmpty();
      assertThat(result.getT2()).isPresent();
      assertThat(result.getT2()).isEqualTo(queryInstant.map(Instant::toEpochMilli));
    }
  }

  public static Stream<Arguments> consumerGroupsLags() {
    String topic = "topic_" + UUID.randomUUID();
    String anotherTopic = "another_topic_" + UUID.randomUUID();
    return Stream.of(
        Arguments.of(
            topic,
            Map.of(
                new TopicPartition(topic, 0), 100L,
                new TopicPartition(topic, 1), 100L,
                new TopicPartition(anotherTopic, 0), 50L,
                new TopicPartition(anotherTopic, 1), 50L
            ),
            10L,
            20L,
            Optional.empty(), Optional.empty(), true
        ),
        Arguments.of(
            topic,
            Map.of(
                new TopicPartition(topic, 0), 100L,
                new TopicPartition(topic, 1), 100L
            ),
            0L,
            0L,
            Optional.empty(), Optional.empty(), true
        ),
        Arguments.of(
            topic,
            Map.of(
                new TopicPartition(topic, 0), 100L,
                new TopicPartition(topic, 1), 100L
            ),
            0L,
            0L,
            Optional.of(Instant.now().minusSeconds(10)),
            Optional.of(Instant.now()),
            false
        )
    );
  }

  @Test
  void getConsumerGroupsFiltersByState() {
    // given
    ClustersProperties.Cluster clusterProperties = new ClustersProperties.Cluster();
    clusterProperties.setName("test");

    ClustersProperties clustersProperties = new ClustersProperties();
    clustersProperties.getClusters().add(clusterProperties);

    KafkaCluster cluster = KafkaCluster.builder()
        .name("test")
        .originalProperties(clusterProperties)
        .build();

    ReactiveAdminClient client = Mockito.mock(ReactiveAdminClient.class);
    AdminClientService admin = Mockito.mock(AdminClientService.class);
    Mockito.when(admin.get(cluster)).thenReturn(Mono.just(client));

    final String topic = UUID.randomUUID().toString();

    // Create consumer groups with different states
    Map<String, ScrapedClusterState.ConsumerGroupState> stableGroups =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(topic, 0)),
            Map.of(new TopicPartition(topic, 0), 100L),
            ConsumerGroupState.STABLE
        )).limit(3).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> emptyGroups =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(topic, 0)),
            Map.of(new TopicPartition(topic, 0), 100L),
            ConsumerGroupState.EMPTY
        )).limit(2).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> deadGroups =
        Stream.generate(() -> generate(
            List.of(new TopicPartition(topic, 0)),
            Map.of(new TopicPartition(topic, 0), 100L),
            ConsumerGroupState.DEAD
        )).limit(4).collect(Collectors.toMap(
            ScrapedClusterState.ConsumerGroupState::group,
            s -> s
        ));

    Map<String, ScrapedClusterState.ConsumerGroupState> allGroups = new HashMap<>();
    allGroups.putAll(stableGroups);
    allGroups.putAll(emptyGroups);
    allGroups.putAll(deadGroups);

    // Mock consumer group listings with state
    Mockito.when(client.listConsumerGroups()).thenReturn(Mono.just(
        allGroups.entrySet().stream()
            .map(e -> new ConsumerGroupListing(
                e.getKey(),
                false,
                Optional.of(e.getValue().description().state())
            ))
            .toList()
    ));

    Mockito.when(client.describeConsumerGroups(Mockito.any())).thenAnswer(invocation -> {
      List<String> groupIds = invocation.getArgument(0);
      return Mono.just(
          groupIds.stream()
              .filter(allGroups::containsKey)
              .collect(Collectors.toMap(
                  id -> id,
                  id -> allGroups.get(id).description()
              ))
      );
    });

    Mockito.when(client.listConsumerGroupOffsets(Mockito.any(), Mockito.isNull())).thenAnswer(
        invocation -> {
          List<String> groupIds = invocation.getArgument(0);
          var table = ImmutableTable.<String, TopicPartition, Long>builder();
          for (String groupId : groupIds) {
            ScrapedClusterState.ConsumerGroupState state = allGroups.get(groupId);
            if (state != null) {
              for (Map.Entry<TopicPartition, Long> entry : state.committedOffsets().entrySet()) {
                table.put(groupId, entry.getKey(), entry.getValue());
              }
            }
          }
          return Mono.just(table.build());
        }
    );

    Mockito.when(client.listOffsets(Mockito.any(), Mockito.any(), Mockito.eq(false)))
        .thenReturn(Mono.just(Map.of(new TopicPartition(topic, 0), 100L)));

    StatisticsCache cache = Mockito.mock(StatisticsCache.class);
    AccessControlService acl = Mockito.mock(AccessControlService.class);
    Mockito.when(acl.isConsumerGroupAccessible(Mockito.any(), Mockito.any()))
        .thenReturn(Mono.just(true));

    ConsumerGroupService service = new ConsumerGroupService(admin, acl, clustersProperties, cache);

    // Test 1: Filter by STABLE state only
    ConsumerGroupService.ConsumerGroupsPage stablePage = service.getConsumerGroups(
        cluster,
        OptionalInt.of(1),
        OptionalInt.of(100),
        null,
        false,
        ConsumerGroupOrderingDTO.NAME,
        SortOrderDTO.ASC,
        List.of(ConsumerGroupStateDTO.STABLE)
    ).block();

    assertThat(stablePage).isNotNull();
    assertThat(stablePage.consumerGroups()).hasSize(3);
    assertThat(stablePage.consumerGroups())
        .allMatch(cg -> cg.getState() == ConsumerGroupState.STABLE);

    // Test 2: Filter by EMPTY state only
    ConsumerGroupService.ConsumerGroupsPage emptyPage = service.getConsumerGroups(
        cluster,
        OptionalInt.of(1),
        OptionalInt.of(100),
        null,
        false,
        ConsumerGroupOrderingDTO.NAME,
        SortOrderDTO.ASC,
        List.of(ConsumerGroupStateDTO.EMPTY)
    ).block();

    assertThat(emptyPage).isNotNull();
    assertThat(emptyPage.consumerGroups()).hasSize(2);
    assertThat(emptyPage.consumerGroups())
        .allMatch(cg -> cg.getState() == ConsumerGroupState.EMPTY);

    // Test 3: Filter by multiple states (STABLE and EMPTY)
    ConsumerGroupService.ConsumerGroupsPage multiStatePage = service.getConsumerGroups(
        cluster,
        OptionalInt.of(1),
        OptionalInt.of(100),
        null,
        false,
        ConsumerGroupOrderingDTO.NAME,
        SortOrderDTO.ASC,
        List.of(ConsumerGroupStateDTO.STABLE, ConsumerGroupStateDTO.EMPTY)
    ).block();

    assertThat(multiStatePage).isNotNull();
    assertThat(multiStatePage.consumerGroups()).hasSize(5);
    assertThat(multiStatePage.consumerGroups())
        .allMatch(cg -> cg.getState() == ConsumerGroupState.STABLE
            || cg.getState() == ConsumerGroupState.EMPTY);

    // Test 4: Empty state filter returns all groups
    ConsumerGroupService.ConsumerGroupsPage allPage = service.getConsumerGroups(
        cluster,
        OptionalInt.of(1),
        OptionalInt.of(100),
        null,
        false,
        ConsumerGroupOrderingDTO.NAME,
        SortOrderDTO.ASC,
        List.of()
    ).block();

    assertThat(allPage).isNotNull();
    assertThat(allPage.consumerGroups()).hasSize(9);
  }
}
