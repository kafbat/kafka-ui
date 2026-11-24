package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.model.InternalLogDirStats.LogDirSpaceStats;
import static io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.index.LazyTopicsIndex;
import io.kafbat.ui.service.index.TopicsIndex;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
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

    // Only the topics that were actually fetched are rebuilt. Rebuilding all topics here made every
    // page listing pay for every partition in the cluster.
    var changedTopics = new ArrayList<InternalTopic>(descriptions.size());
    var previousTopics = topicIndex.topicByName();
    descriptions.forEach((topic, description) -> {
      var newState = updatedTopicStates.get(topic);
      changedTopics.add(reusedOrBuiltTopic(newState, topicStates.get(topic),
          previousTopics.get(topic), clustersProperties));
    });
    topicIndex.updateTopics(changedTopics);
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
                                                 LazyTopicsIndex topicIndex,
                                                 Map<String, TopicState> previousTopicStates) {
    // Both describeTopics() and getTopicsConfig() list all topic names on their own. Do it once and
    // share the result, so a scrape of a 15k-topic cluster does not fetch the full name list twice.
    return ac.listTopics(true).flatMap(topicNames ->
        Mono.zip(
            ac.describeLogDirs(clusterDescription.getNodes().stream().map(Node::id).toList())
                .map(InternalLogDirStats::new),
            ac.listConsumerGroups().map(l -> l.stream().map(ConsumerGroupListing::groupId).toList()),
            ac.describeTopics(topicNames),
            ac.getTopicsConfig(topicNames, false)
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
                    new ConsumerStates(phase2.getT3(), phase2.getT4()),
                    clustersProperties,
                    topicIndex,
                    previousTopicStates
                ))));
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

  private record ConsumerStates(
      Map<String, ConsumerGroupDescription> descriptions,
      Table<String, TopicPartition, Long> offsets) {}

  private static ScrapedClusterState create(ClusterDescription clusterDescription,
                                            InternalLogDirStats segmentStats,
                                            Map<String, TopicState> topicStates,
                                            ConsumerStates consumerStates,
                                            ClustersProperties clustersProperties,
                                            LazyTopicsIndex topicIndex,
                                            Map<String, TopicState> previousTopicStates) {

    Map<String, ConsumerGroupState> consumerGroupsStates = new HashMap<>();
    consumerStates.descriptions().forEach((name, desc) ->
        consumerGroupsStates.put(
            name,
            new ConsumerGroupState(
                name,
                desc,
                consumerStates.offsets().row(name)
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

    topicIndex.update(internalTopics(topicStates, clustersProperties, previousTopicStates,
        topicIndex.topicByName()));

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
    return internalTopics(topicStates, clustersProperties, Map.of(), Map.of());
  }

  /**
   * Rebuilds only the topics whose source data changed. Offsets grow between scrapes, so this is not
   * always possible, but idle topics and topics whose data was refreshed by a page request keep their
   * existing {@link InternalTopic} objects along with all their partition structures.
   */
  private static List<InternalTopic> internalTopics(Map<String, TopicState> topicStates,
                                                   ClustersProperties clustersProperties,
                                                   Map<String, TopicState> previousTopicStates,
                                                   Map<String, InternalTopic> existingTopics) {
    return topicStates.values().stream()
        .map(state -> reusedOrBuiltTopic(state, previousTopicStates.get(state.name()),
            existingTopics.get(state.name()), clustersProperties))
        .toList();
  }

  /**
   * Reuses the cached topic when its definition is unchanged, but refreshes the metric fields that
   * are read from offsets and log dir stats. This keeps size/message-count sorting accurate without
   * rebuilding every partition, replica and config object on every scrape.
   */
  static InternalTopic reusedOrBuiltTopic(TopicState state,
                                          @Nullable TopicState previousState,
                                          @Nullable InternalTopic existingTopic,
                                          ClustersProperties clustersProperties) {
    if (previousState == null || existingTopic == null || !sameTopicDefinition(previousState, state)) {
      return buildInternalTopic(state, clustersProperties);
    }
    return withRefreshedMetrics(existingTopic, state);
  }

  private static boolean sameTopicDefinition(TopicState previous, TopicState current) {
    return previous.name().equals(current.name())
        && previous.description().equals(current.description())
        && configsByName(previous.configs()).equals(configsByName(current.configs()));
  }

  private static Map<String, String> configsByName(List<ConfigEntry> configs) {
    Map<String, String> result = new HashMap<>(configs.size());
    configs.forEach(config -> result.put(config.name(), config.value()));
    return result;
  }

  private static InternalTopic withRefreshedMetrics(InternalTopic topic, TopicState state) {
    var partitions = new HashMap<Integer, InternalPartition>();
    if (topic.getPartitions() != null) {
      partitions.putAll(topic.getPartitions());
    }
    var partitionsSegmentStats = state.partitionsSegmentStats();
    state.description().partitions().forEach(partition -> {
      int partitionId = partition.partition();
      var cached = partitions.get(partitionId);
      if (cached == null) {
        return;
      }
      SegmentStats partitionStats = partitionsSegmentStats == null
          ? null
          : partitionsSegmentStats.get(partitionId);
      partitions.put(partitionId, cached.toBuilder()
          .offsetMin(state.startOffsets().get(partitionId))
          .offsetMax(state.endOffsets().get(partitionId))
          .segmentSize(partitionStats == null ? null : partitionStats.getSegmentSize())
          .segmentCount(partitionStats == null ? null : partitionStats.getSegmentsCount())
          .build());
    });

    SegmentStats topicStats = state.segmentStats();
    return topic.toBuilder()
        .partitions(partitions)
        .segmentSize(topicStats == null ? 0 : topicStats.getSegmentSize())
        .segmentCount(topicStats == null ? 0 : topicStats.getSegmentsCount())
        .build();
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
