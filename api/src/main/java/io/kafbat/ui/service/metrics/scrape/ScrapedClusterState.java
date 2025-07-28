package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import com.google.common.collect.Table;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.model.state.ConsumerGroupState;
import io.kafbat.ui.model.state.NodeState;
import io.kafbat.ui.model.state.TopicState;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.index.TopicsIndex;
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
  TopicsIndex topicsIndex;


  public static ScrapedClusterState empty() {
    return ScrapedClusterState.builder()
        .scrapeFinishedAt(Instant.now())
        .nodesStates(Map.of())
        .topicStates(Map.of())
        .consumerGroupsStates(Map.of())
        .build();
  }

  public ScrapedClusterState updateTopics(Map<String, TopicDescription> descriptions,
                                          Map<String, List<ConfigEntry>> configs,
                                          InternalPartitionsOffsets partitionsOffsets) {
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
    return toBuilder()
        .topicStates(updatedTopicStates)
        .build();
  }

  public ScrapedClusterState topicDeleted(String topic) {
    var newTopicStates = new HashMap<>(topicStates);
    newTopicStates.remove(topic);
    return toBuilder()
        .topicStates(newTopicStates)
        .build();
  }

  public static Mono<ScrapedClusterState> scrape(ClusterDescription clusterDescription,
                                                 ClustersProperties clustersProperties,
                                                 ReactiveAdminClient ac) {
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
        ).flatMap(phase2 ->
            create(
                clusterDescription,
                phase1.getT1(),
                topicStateMap(phase1.getT1(), phase1.getT3(), phase1.getT4(), phase2.getT1(), phase2.getT2()),
                phase2.getT3(),
                phase2.getT4(),
                clustersProperties
            )));
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

  private static Mono<ScrapedClusterState> create(ClusterDescription clusterDescription,
                                            InternalLogDirStats segmentStats,
                                            Map<String, TopicState> topicStates,
                                            Map<String, ConsumerGroupDescription> consumerDescriptions,
                                            Table<String, TopicPartition, Long> consumerOffsets,
                                            ClustersProperties clustersProperties) {
    try {
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

      TopicsIndex topicsIndex = new TopicsIndex(topicStates.entrySet().stream().map(e ->
          buildInternalTopic(e.getValue(), clustersProperties)
      ).toList());

      return Mono.just(new ScrapedClusterState(
          Instant.now(),
          nodesStates,
          topicStates,
          consumerGroupsStates,
          topicsIndex
      ));
    } catch (Exception e) {
      return Mono.error(new RuntimeException("Error scrapping cluster", e));
    }
  }

  private static InternalTopic buildInternalTopic(TopicState state, ClustersProperties clustersProperties) {
    return InternalTopic.from(
        state.description(),
        state.configs(),
        InternalPartitionsOffsets.empty(),
        null,
        state.segmentStats(),
        state.partitionsSegmentStats(),
        clustersProperties.getInternalTopicPrefix()
    );
  }

  private static <T> Map<Integer, T> filterTopic(String topicForFilter, Map<TopicPartition, T> tpMap) {
    return tpMap.entrySet()
        .stream()
        .filter(tp -> tp.getKey().topic().equals(topicForFilter))
        .collect(Collectors.toMap(e -> e.getKey().partition(), Map.Entry::getValue));
  }


  @Override
  public void close() throws Exception {
    if (this.topicsIndex != null) {
      this.topicsIndex.close();
    }
  }
}
