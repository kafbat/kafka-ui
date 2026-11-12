package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class ScrapedClusterStateTest {

  @Test
  void emptyStateHasNonNullTopicIndex() throws Exception {
    try (ScrapedClusterState empty = ScrapedClusterState.empty()) {
      assertThat(empty.getTopicIndex()).isNotNull();
      assertThat(empty.getTopicIndex().find(null, null, false, null)).isEmpty();
      assertThat(empty.getTopicIndex().find("search", true, true, null)).isEmpty();
    }
  }

  @Test
  void topicStateMapGroupsOffsetsAndStatsPerTopic() {
    InternalLogDirStats segmentStats = new InternalLogDirStats(Map.of(
        1, Map.of("/data", new LogDirDescription(null, Map.of(
            new TopicPartition("a", 0), new ReplicaInfo(10L, 0L, false),
            new TopicPartition("a", 1), new ReplicaInfo(20L, 0L, false),
            new TopicPartition("b", 0), new ReplicaInfo(30L, 0L, false)
        )))
    ));

    Map<String, TopicDescription> descriptions = Map.of(
        "a", topicDescription("a"),
        "b", topicDescription("b")
    );
    Map<String, List<ConfigEntry>> configs = Map.of(
        "a", List.of(new ConfigEntry("retention.ms", "1000"))
    );
    Map<TopicPartition, Long> latest = Map.of(
        new TopicPartition("a", 0), 100L,
        new TopicPartition("a", 1), 200L,
        new TopicPartition("b", 0), 300L
    );
    Map<TopicPartition, Long> earliest = Map.of(
        new TopicPartition("a", 0), 0L,
        new TopicPartition("a", 1), 5L,
        new TopicPartition("b", 0), 10L
    );

    Map<String, ScrapedClusterState.TopicState> result =
        ScrapedClusterState.topicStateMap(segmentStats, descriptions, configs, latest, earliest);

    assertThat(result).containsOnlyKeys("a", "b");

    var a = result.get("a");
    assertThat(a.name()).isEqualTo("a");
    assertThat(a.description()).isSameAs(descriptions.get("a"));
    assertThat(a.configs()).containsExactly(new ConfigEntry("retention.ms", "1000"));
    assertThat(a.startOffsets()).containsOnly(Map.entry(0, 0L), Map.entry(1, 5L));
    assertThat(a.endOffsets()).containsOnly(Map.entry(0, 100L), Map.entry(1, 200L));
    assertThat(a.segmentStats()).isEqualTo(new SegmentStats(30L, 2));
    assertThat(a.partitionsSegmentStats())
        .containsOnly(Map.entry(0, new SegmentStats(10L, 1)), Map.entry(1, new SegmentStats(20L, 1)));

    var b = result.get("b");
    assertThat(b.configs()).isEmpty();
    assertThat(b.startOffsets()).containsOnly(Map.entry(0, 10L));
    assertThat(b.endOffsets()).containsOnly(Map.entry(0, 300L));
    assertThat(b.segmentStats()).isEqualTo(new SegmentStats(30L, 1));
    assertThat(b.partitionsSegmentStats()).containsOnly(Map.entry(0, new SegmentStats(30L, 1)));
  }

  private static TopicDescription topicDescription(String name) {
    return new TopicDescription(name, false, List.of());
  }
}
