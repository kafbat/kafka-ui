package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.InternalLogDirStats;
import io.kafbat.ui.model.InternalLogDirStats.SegmentStats;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConfigEntry;
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
  void topicStateMapGroupsOffsetsAndStatsPerTopic() throws Exception {
    SegmentStats partA0 = new SegmentStats(10L, 1);
    SegmentStats partA1 = new SegmentStats(20L, 2);
    SegmentStats partB0 = new SegmentStats(30L, 3);
    SegmentStats topicAStats = new SegmentStats(30L, 3);
    SegmentStats topicBStats = new SegmentStats(30L, 3);

    InternalLogDirStats segmentStats = buildLogDirStats(
        Map.of(
            new TopicPartition("a", 0), partA0,
            new TopicPartition("a", 1), partA1,
            new TopicPartition("b", 0), partB0
        ),
        Map.of("a", topicAStats, "b", topicBStats)
    );

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
    assertThat(a.segmentStats()).isEqualTo(topicAStats);
    assertThat(a.partitionsSegmentStats()).containsOnly(Map.entry(0, partA0), Map.entry(1, partA1));

    var b = result.get("b");
    assertThat(b.configs()).isEmpty();
    assertThat(b.startOffsets()).containsOnly(Map.entry(0, 10L));
    assertThat(b.endOffsets()).containsOnly(Map.entry(0, 300L));
    assertThat(b.segmentStats()).isEqualTo(topicBStats);
    assertThat(b.partitionsSegmentStats()).containsOnly(Map.entry(0, partB0));
  }

  private static TopicDescription topicDescription(String name) {
    return new TopicDescription(name, false, List.of());
  }

  // InternalLogDirStats is @Value with an explicit 1-arg constructor, so Lombok does not
  // generate an all-args constructor. Reflection lets tests set the two fields topicStateMap
  // actually reads without constructing a full Map<Integer, Map<String, LogDirDescription>>.
  private static InternalLogDirStats buildLogDirStats(
      Map<TopicPartition, SegmentStats> partitionsStats,
      Map<String, SegmentStats> topicStats) throws Exception {
    InternalLogDirStats instance = InternalLogDirStats.empty();
    setField(instance, "partitionsStats", partitionsStats);
    setField(instance, "topicStats", topicStats);
    return instance;
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
