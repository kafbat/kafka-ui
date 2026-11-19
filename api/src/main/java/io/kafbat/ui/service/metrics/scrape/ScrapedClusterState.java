package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.model.InternalLogDirStats.LogDirSpaceStats;
import static io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.index.FilterTopicIndex;
import io.kafbat.ui.service.index.LuceneTopicsIndex;
import io.kafbat.ui.service.index.TopicsIndex;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ScrapedClusterState implements AutoCloseable {

  Instant scrapeFinishedAt;
  // When the topic configs held in topicStates were last fully re-described. Deliberately separate from
  // scrapeFinishedAt, which create() refreshes on every scrape (and which ConsumerGroupService reads to report
  // offset freshness) - reusing it would restart the expiry window every scrape and the refresh would never fire.
  Instant topicConfigsRefreshedAt;
  Map<Integer, NodeState> nodesStates;
  Map<String, TopicState> topicStates;
  Map<String, ConsumerGroupState> consumerGroupsStates;
  TopicsIndex topicIndex;

  @Override
  public void close() throws Exception {
    if (this.topicIndex != null) {
      this.topicIndex.close();
    }
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
        // EPOCH rather than now(), so the very first real scrape always describes configs
        .topicConfigsRefreshedAt(Instant.EPOCH)
        .nodesStates(Map.of())
        .topicStates(Map.of())
        .consumerGroupsStates(Map.of())
        .topicIndex(new FilterTopicIndex(List.of()))
        .build();
  }

  // Freshly fetched configs win, but a topic that is absent from (or empty in) the fetched map keeps whatever we
  // already knew about it. getTopicsConfigImpl() deliberately swallows per-resource errors such as
  // TopicAuthorizationException, so without this a partial failure blanks those topics' configs, which silently
  // drops their cleanUpPolicy to UNKNOWN and nulls messagesCount in the topics list.
  @VisibleForTesting
  static Map<String, List<ConfigEntry>> mergeTopicConfigs(Map<String, TopicState> previousStates,
                                                          Map<String, List<ConfigEntry>> fetched) {
    Map<String, List<ConfigEntry>> merged = new HashMap<>();
    previousStates.forEach((topic, state) -> {
      if (state.configs() != null && !state.configs().isEmpty()) {
        merged.put(topic, state.configs());
      }
    });
    fetched.forEach((topic, configs) -> {
      if (configs != null && !configs.isEmpty()) {
        merged.put(topic, configs);
      }
    });
    return merged;
  }

  public ScrapedClusterState updateTopics(Map<String, TopicDescription> descriptions,
                                          Map<String, List<ConfigEntry>> configs,
                                          InternalPartitionsOffsets partitionsOffsets,
                                          ClustersProperties clustersProperties) {
    Map<String, List<ConfigEntry>> mergedConfigs = mergeTopicConfigs(topicStates, configs);
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
              mergedConfigs.getOrDefault(topic, List.of()),
              partitionsOffsets.topicOffsets(topic, true),
              partitionsOffsets.topicOffsets(topic, false),
              segmentStats,
              partitionsSegmentStats
          )
      );
    });

    return toBuilder()
        .topicStates(updatedTopicStates)
        .topicIndex(buildTopicIndex(clustersProperties, updatedTopicStates))
        .build();
  }

  public ScrapedClusterState topicDeleted(String topic) {
    var newTopicStates = new HashMap<>(topicStates);
    newTopicStates.remove(topic);
    return toBuilder()
        .topicStates(newTopicStates)
        .build();
  }

  // Which topics need a live describeConfigs on this scrape, and whether that amounts to a full refresh.
  // Kept as a pure function of (previous state, current topics, expiry, now) so the policy can be tested
  // without introducing a Clock seam into the scrape.
  @VisibleForTesting
  record TopicConfigsRefreshPlan(Set<String> topicsToDescribe, boolean fullRefresh) {

    static TopicConfigsRefreshPlan plan(ScrapedClusterState previous,
                                        Set<String> currentTopics,
                                        Duration expiry,
                                        Instant now) {
      Instant refreshedAt = previous.getTopicConfigsRefreshedAt();
      if (expiry == null || expiry.isZero() || expiry.isNegative()
          || refreshedAt == null || !now.isBefore(refreshedAt.plus(expiry))) {
        return new TopicConfigsRefreshPlan(currentTopics, true);
      }
      // Only topics we have not seen before. Keyed on presence in topicStates rather than on having non-empty
      // configs: getTopicsConfigImpl() swallows TopicAuthorizationException, so keying on content would
      // re-describe every DESCRIBE_CONFIGS-denied topic on every scrape - the storm this setting exists to stop.
      Set<String> unseen = new HashSet<>(currentTopics);
      unseen.removeAll(previous.getTopicStates().keySet());
      return new TopicConfigsRefreshPlan(unseen, false);
    }

    Mono<Map<String, List<ConfigEntry>>> fetch(ReactiveAdminClient ac) {
      return topicsToDescribe.isEmpty() ? Mono.just(Map.of()) : ac.getTopicsConfig(topicsToDescribe, false);
    }

    // An incremental fetch must not advance the clock, or a cluster with steady topic churn would keep
    // postponing the full refresh forever.
    Instant refreshedAt(Instant previousRefreshedAt, Instant now) {
      return fullRefresh ? now : previousRefreshedAt;
    }
  }

  public static Mono<ScrapedClusterState> scrape(ClusterDescription clusterDescription,
                                                 ReactiveAdminClient ac,
                                                 ClustersProperties clustersProperties,
                                                 ScrapedClusterState previous,
                                                 Duration topicConfigsExpiry) {
    Instant now = Instant.now();
    // One listTopics() for both describeTopics() and the config fetch, so a topic created mid-scrape cannot end
    // up described but config-less, and so the refresh plan can be decided before anything is described.
    return ac.listTopics(true).flatMap(topics -> {
      TopicConfigsRefreshPlan plan = TopicConfigsRefreshPlan.plan(previous, topics, topicConfigsExpiry, now);
      return Mono.zip(
          ac.describeLogDirs(clusterDescription.getNodes().stream().map(Node::id).toList())
              .map(InternalLogDirStats::new),
          ac.listConsumerGroups().map(l -> l.stream().map(ConsumerGroupListing::groupId).toList()),
          ac.describeTopics(topics),
          plan.fetch(ac)
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
                  topicStateMap(phase1.getT1(), phase1.getT3(),
                      mergeTopicConfigs(previous.getTopicStates(), phase1.getT4()),
                      phase2.getT1(), phase2.getT2()),
                  phase2.getT3(),
                  phase2.getT4(),
                  clustersProperties,
                  plan.refreshedAt(previous.getTopicConfigsRefreshedAt(), now)
              )));
    });
  }

  private static Map<String, TopicState> topicStateMap(
      InternalLogDirStats segmentStats,
      Map<String, TopicDescription> topicDescriptions,
      Map<String, List<ConfigEntry>> topicConfigs,
      Map<TopicPartition, Long> latestOffsets,
      Map<TopicPartition, Long> earliestOffsets) {

    return topicDescriptions.entrySet().stream().map(entry -> new TopicState(
        entry.getKey(),
        entry.getValue(),
        topicConfigs.getOrDefault(entry.getKey(), List.of()),
        filterTopic(entry.getKey(), earliestOffsets),
        filterTopic(entry.getKey(), latestOffsets),
        segmentStats.getTopicStats().get(entry.getKey()),
        Optional.ofNullable(segmentStats.getPartitionsStats())
            .map(topicForFilter -> filterTopic(entry.getKey(), topicForFilter))
            .orElse(null)
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
                                            Instant topicConfigsRefreshedAt) {

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

    return new ScrapedClusterState(
        Instant.now(),
        topicConfigsRefreshedAt,
        nodesStates,
        topicStates,
        consumerGroupsStates,
        buildTopicIndex(clustersProperties, topicStates)
    );
  }

  private static TopicsIndex buildTopicIndex(ClustersProperties clustersProperties,
                                             Map<String, TopicState> topicStates) {
    ClustersProperties.ClusterFtsProperties fts = clustersProperties.getFts();
    List<InternalTopic> topics = topicStates.values().stream().map(
        topicState -> buildInternalTopic(topicState, clustersProperties)
    ).toList();

    if (fts.isEnabled()) {
      try {
        return new LuceneTopicsIndex(topics);
      } catch (Exception e) {
        log.error("Error creating lucene topics index", e);
      }
    }
    return new FilterTopicIndex(topics);
  }

  private static <T> Map<Integer, T> filterTopic(String topicForFilter, Map<TopicPartition, T> tpMap) {
    return tpMap.entrySet()
        .stream()
        .filter(tp -> tp.getKey().topic().equals(topicForFilter))
        .collect(Collectors.toMap(e -> e.getKey().partition(), Map.Entry::getValue));
  }

  private static InternalTopic buildInternalTopic(TopicState state,
                                                  ClustersProperties clustersProperties) {
    return InternalTopic.from(state, clustersProperties.getInternalTopicPrefix());
  }
}
