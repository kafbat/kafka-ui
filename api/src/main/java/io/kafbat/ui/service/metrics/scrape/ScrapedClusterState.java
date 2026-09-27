package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.model.InternalLogDirStats.LogDirSpaceStats;
import static io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.index.LazyTopicsIndex;
import io.kafbat.ui.service.index.TopicsIndex;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import reactor.core.publisher.Mono;

@Builder(toBuilder = true)
@RequiredArgsConstructor
@Value
public class ScrapedClusterState implements AutoCloseable {

  Instant scrapeFinishedAt;
  Map<Integer, NodeState> nodesStates;
  Map<String, TopicState> topicStates;
  Map<String, ConsumerGroupState> consumerGroupsStates;
  TopicsIndex topicIndex;

  @Override
  public void close() throws Exception {
    // the topic index is shared between all snapshots of a cluster and is owned by
    // TopicsIndexRegistry, which releases it on shutdown
  }

  public record NodeState(int id,
                          Node node,
                          @Nullable SegmentStats segmentStats,
                          @Nullable LogDirSpaceStats logDirSpaceStats) {
  }

  public record TopicState(
      String name,
      TopicDescription description,
      List<ConfigEntry> configs,
      Map<Integer, Long> startOffsets,
      Map<Integer, Long> endOffsets,
      @Nullable SegmentStats segmentStats,
      @Nullable Map<Integer, SegmentStats> partitionsSegmentStats) {
  }

  public record ConsumerGroupState(
      String group,
      ConsumerGroupDescription description,
      Map<TopicPartition, Long> committedOffsets) {
  }

  public static ScrapedClusterState empty() {
    return ScrapedClusterState.builder()
        .scrapeFinishedAt(Instant.now())
        .nodesStates(Map.of())
        .topicStates(Map.of())
        .consumerGroupsStates(Map.of())
        .topicIndex(new LazyTopicsIndex(List.of()))
        .build();
  }

  public ScrapedClusterState updateTopics(Map<String, TopicDescription> descriptions,
                                          Map<String, List<ConfigEntry>> configs,
                                          InternalPartitionsOffsets partitionsOffsets,
                                          ClustersProperties clustersProperties,
                                          LazyTopicsIndex topicIndex) {
    var updatedTopicStates = new HashMap<>(topicStates);
    descriptions.forEach((topic, description) -> {
      SegmentStats segmentStats = null;
      Map<Integer, SegmentStats> partitionsSegmentStats = null;
      if (topicStates.containsKey(topic)) {
        segmentStats = topicStates.get(topic).segmentStats();
        partitionsSegmentStats = topicStates.get(topic).partitionsSegmentStats();
      }
      updatedTopicStates.put(
          topic,
          new TopicState(
              topic,
              description,
              configs.getOrDefault(topic, List.of()),
              partitionsOffsets.topicOffsets(topic, true),
              partitionsOffsets.topicOffsets(topic, false),
              segmentStats,
              partitionsSegmentStats
          )
      );
    });

    topicIndex.update(internalTopics(updatedTopicStates, clustersProperties));
    return toBuilder()
        .topicStates(updatedTopicStates)
        .topicIndex(topicIndex)
        .build();
  }

  public ScrapedClusterState topicDeleted(String topic, ClustersProperties clustersProperties,
                                          LazyTopicsIndex topicIndex) {
    var newTopicStates = new HashMap<>(topicStates);
    newTopicStates.remove(topic);
    topicIndex.update(internalTopics(newTopicStates, clustersProperties));
    return toBuilder()
        .topicStates(newTopicStates)
        .topicIndex(topicIndex)
        .build();
  }

  public static Mono<ScrapedClusterState> scrape(ClusterDescription clusterDescription,
                                                 ReactiveAdminClient ac, ClustersProperties clustersProperties,
                                                 LazyTopicsIndex topicIndex) {
    return Mono.zip(
        ac.describeLogDirs(clusterDescription.getNodes().stream().map(Node::id).toList())
            .map(InternalLogDirStats::new),
        ac.listConsumerGroups().map(l -> l.stream().map(ConsumerGroupListing::groupId).toList()),
        ac.describeTopics(),
        ac.getTopicsConfig()
    ).flatMap(phase1 ->
        Mono.zip(
            ac.listOffsets(phase1.getT3().values(), OffsetSpec.latest()),
            ac.listOffsets(phase1.getT3().values(), OffsetSpec.earliest()),
            ac.describeConsumerGroups(phase1.getT2()),
            ac.listConsumerGroupOffsets(phase1.getT2(), null)
        ).map(phase2 ->
            create(
                clusterDescription,
                phase1.getT1(),
                topicStateMap(phase1.getT1(), phase1.getT3(), phase1.getT4(), phase2.getT1(), phase2.getT2()),
                phase2.getT3(),
                phase2.getT4(),
                clustersProperties,
                topicIndex
            )));
  }

  static Map<String, TopicState> topicStateMap(
      InternalLogDirStats segmentStats,
      Map<String, TopicDescription> topicDescriptions,
      Map<String, List<ConfigEntry>> topicConfigs,
      Map<TopicPartition, Long> latestOffsets,
      Map<TopicPartition, Long> earliestOffsets) {

    var earliestByTopic = groupPartitionsByTopic(earliestOffsets);
    var latestByTopic = groupPartitionsByTopic(latestOffsets);
    var partitionsStatsByTopic = Optional.ofNullable(segmentStats.getPartitionsStats())
        .map(ScrapedClusterState::groupPartitionsByTopic)
        .orElse(null);

    return topicDescriptions.entrySet().stream().map(entry -> new TopicState(
        entry.getKey(),
        entry.getValue(),
        topicConfigs.getOrDefault(entry.getKey(), List.of()),
        earliestByTopic.getOrDefault(entry.getKey(), Map.of()),
        latestByTopic.getOrDefault(entry.getKey(), Map.of()),
        segmentStats.getTopicStats().get(entry.getKey()),
        partitionsStatsByTopic == null
            ? null
            : partitionsStatsByTopic.getOrDefault(entry.getKey(), Map.of())
    )).collect(Collectors.toMap(
        TopicState::name,
        Function.identity()
    ));
  }

  private static ScrapedClusterState create(ClusterDescription clusterDescription,
                                            InternalLogDirStats segmentStats,
                                            Map<String, TopicState> topicStates,
                                            Map<String, ConsumerGroupDescription> consumerDescriptions,
                                            Table<String, TopicPartition, Long> consumerOffsets,
                                            ClustersProperties clustersProperties,
                                            LazyTopicsIndex topicIndex) {

    Map<String, ConsumerGroupState> consumerGroupsStates = new HashMap<>();
    consumerDescriptions.forEach((name, desc) ->
        consumerGroupsStates.put(
            name,
            new ConsumerGroupState(
                name,
                desc,
                consumerOffsets.row(name)
            )));

    Map<Integer, NodeState> nodesStates = new HashMap<>();
    clusterDescription.getNodes().forEach(node ->
        nodesStates.put(
            node.id(),
            new NodeState(
                node.id(),
                node,
                segmentStats.getBrokerStats().get(node.id()),
                segmentStats.getBrokerDirsStats().get(node.id())
            )));

    topicIndex.update(internalTopics(topicStates, clustersProperties));

    return new ScrapedClusterState(
        Instant.now(),
        nodesStates,
        topicStates,
        consumerGroupsStates,
        topicIndex
    );
  }

  private static List<InternalTopic> internalTopics(Map<String, TopicState> topicStates,
                                                   ClustersProperties clustersProperties) {
    return topicStates.values().stream().map(
        topicState -> buildInternalTopic(topicState, clustersProperties)
    ).toList();
  }

  /**
   * Groups partition-keyed data by topic once, so that per-topic lookups stay O(partitions of that topic)
   * instead of scanning every partition in the cluster for every topic.
   */
  private static <T> Map<String, Map<Integer, T>> groupPartitionsByTopic(Map<TopicPartition, T> tpMap) {
    return tpMap.entrySet().stream().collect(Collectors.groupingBy(
        entry -> entry.getKey().topic(),
        Collectors.toMap(entry -> entry.getKey().partition(), Map.Entry::getValue)
    ));
  }

  private static InternalTopic buildInternalTopic(TopicState state,
                                                  ClustersProperties clustersProperties) {
    return InternalTopic.from(state, clustersProperties.getInternalTopicPrefix());
  }
}
