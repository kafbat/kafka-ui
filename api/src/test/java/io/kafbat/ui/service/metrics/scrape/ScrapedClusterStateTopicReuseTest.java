package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.CleanupPolicy;
import io.kafbat.ui.model.InternalTopic;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

class ScrapedClusterStateTopicReuseTest {

  private static final String TOPIC = "orders";
  private static final Node BROKER = new Node(1, "localhost", 9092);

  @Test
  void refreshesMetricsWithoutRebuildingUnchangedTopicStructure() {
    var oldState = state(2, configs("delete"), 0, 10, new SegmentStats(100L, 2));
    var oldTopic = InternalTopic.from(oldState, null);
    var oldReplicas = oldTopic.getPartitions().values().stream()
        .map(partition -> partition.getReplicas().get(0))
        .collect(Collectors.toList());

    var newState = state(2, configs("delete"), 0, 20, new SegmentStats(300L, 3));
    var newTopic = ScrapedClusterState.reusedOrBuiltTopic(
        newState, oldState, oldTopic, new ClustersProperties());

    assertThat(newTopic.getPartitions().get(0).getOffsetMin()).isEqualTo(0L);
    assertThat(newTopic.getPartitions().get(0).getOffsetMax()).isEqualTo(20L);
    assertThat(newTopic.getPartitions().get(1).getOffsetMax()).isEqualTo(20L);
    assertThat(newTopic.getSegmentSize()).isEqualTo(300L);
    assertThat(newTopic.getSegmentCount()).isEqualTo(3L);
    assertThat(newTopic.getPartitions().values().stream()
        .map(partition -> partition.getReplicas().get(0))
        .collect(Collectors.toList()))
        .containsExactlyInAnyOrderElementsOf(oldReplicas);
  }

  @Test
  void treatsConfigsInDifferentOrderAsUnchanged() {
    var oldConfigs = List.of(
        new ConfigEntry(CLEANUP_POLICY_CONFIG, "delete"),
        new ConfigEntry("retention.ms", "3600000"));
    var newConfigs = List.of(
        new ConfigEntry("retention.ms", "3600000"),
        new ConfigEntry(CLEANUP_POLICY_CONFIG, "delete"));
    var oldState = state(1, oldConfigs, 0, 10, new SegmentStats(100L, 1));
    var oldTopic = InternalTopic.from(oldState, null);

    var newTopic = ScrapedClusterState.reusedOrBuiltTopic(
        state(1, newConfigs, 0, 11, new SegmentStats(100L, 1)),
        oldState, oldTopic, new ClustersProperties());

    assertThat(newTopic.getPartitions().get(0).getReplicas().get(0))
        .isSameAs(oldTopic.getPartitions().get(0).getReplicas().get(0));
    assertThat(newTopic.getPartitions().get(0).getOffsetMax()).isEqualTo(11L);
  }

  @Test
  void rebuildsTopicWhenConfigValueChanged() {
    var oldState = state(1, configs("delete"), 0, 10, new SegmentStats(100L, 1));
    var oldTopic = InternalTopic.from(oldState, null);

    var newTopic = ScrapedClusterState.reusedOrBuiltTopic(
        state(1, configs("compact"), 0, 10, new SegmentStats(100L, 1)),
        oldState, oldTopic, new ClustersProperties());

    assertThat(newTopic).isNotSameAs(oldTopic);
    assertThat(newTopic.getPartitions().get(0).getReplicas().get(0))
        .isNotSameAs(oldTopic.getPartitions().get(0).getReplicas().get(0));
    assertThat(newTopic.getCleanUpPolicy()).isEqualTo(CleanupPolicy.COMPACT);
  }

  @Test
  void rebuildsTopicWhenPartitionCountChanged() {
    var oldState = state(1, configs("delete"), 0, 10, new SegmentStats(100L, 1));
    var oldTopic = InternalTopic.from(oldState, null);
    var newState = state(2, configs("delete"), 0, 10, new SegmentStats(200L, 2));

    var newTopic = ScrapedClusterState.reusedOrBuiltTopic(
        newState, oldState, oldTopic, new ClustersProperties());

    assertThat(newTopic).isNotSameAs(oldTopic);
    assertThat(newTopic.getPartitionCount()).isEqualTo(2);
    assertThat(newTopic.getPartitions().keySet()).containsExactlyInAnyOrder(0, 1);
  }

  private static ScrapedClusterState.TopicState state(int partitions,
                                                      List<ConfigEntry> configs,
                                                      long earliest,
                                                      long latest,
                                                      SegmentStats segmentStats) {
    var description = description(partitions);
    return new ScrapedClusterState.TopicState(
        TOPIC,
        description,
        configs,
        offsets(partitions, earliest),
        offsets(partitions, latest),
        segmentStats,
        IntStream.range(0, partitions).boxed()
            .collect(Collectors.toMap(id -> id, id -> segmentStats)));
  }

  private static TopicDescription description(int partitions) {
    var partitionInfos = IntStream.range(0, partitions)
        .mapToObj(id -> new TopicPartitionInfo(id, BROKER, List.of(BROKER), List.of(BROKER)))
        .toList();
    return new TopicDescription(TOPIC, false, partitionInfos);
  }

  private static Map<Integer, Long> offsets(int partitions, long value) {
    return IntStream.range(0, partitions).boxed().collect(Collectors.toMap(id -> id, id -> value));
  }

  private static List<ConfigEntry> configs(String cleanupPolicy) {
    return List.of(new ConfigEntry(CLEANUP_POLICY_CONFIG, cleanupPolicy));
  }
}
