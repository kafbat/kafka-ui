package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.InternalLogDirStats;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class ScrapedClusterStateTopicStateMapTest {

  private static final List<String> TOPIC_NAMES = List.of("alpha", "beta", "gamma");

  private static Map<String, TopicDescription> descriptions() {
    return TOPIC_NAMES.stream().collect(java.util.stream.Collectors.toMap(
        name -> name,
        name -> new TopicDescription(name, false, List.of())
    ));
  }

  @Test
  void offsetsAreGroupedPerTopic() {
    var latest = Map.of(
        new TopicPartition("alpha", 0), 100L,
        new TopicPartition("alpha", 1), 110L,
        new TopicPartition("beta", 0), 200L,
        new TopicPartition("gamma", 3), 300L
    );
    var earliest = Map.of(
        new TopicPartition("alpha", 0), 1L,
        new TopicPartition("beta", 0), 2L
    );

    var states = ScrapedClusterState.topicStateMap(
        InternalLogDirStats.empty(),
        descriptions(),
        Map.of("alpha", List.of(new ConfigEntry("retention.ms", "1000"))),
        latest,
        earliest
    );

    assertThat(states).containsOnlyKeys("alpha", "beta", "gamma");

    assertThat(states.get("alpha").endOffsets()).containsExactlyInAnyOrderEntriesOf(Map.of(0, 100L, 1, 110L));
    assertThat(states.get("alpha").startOffsets()).containsExactlyInAnyOrderEntriesOf(Map.of(0, 1L));

    assertThat(states.get("beta").endOffsets()).containsExactlyInAnyOrderEntriesOf(Map.of(0, 200L));
    assertThat(states.get("beta").startOffsets()).containsExactlyInAnyOrderEntriesOf(Map.of(0, 2L));

    // gamma has a latest offset but no earliest offset
    assertThat(states.get("gamma").endOffsets()).containsExactlyInAnyOrderEntriesOf(Map.of(3, 300L));
    assertThat(states.get("gamma").startOffsets()).isEmpty();

    assertThat(states.get("alpha").configs()).extracting(ConfigEntry::name).containsExactly("retention.ms");
    assertThat(states.get("beta").configs()).isEmpty();
  }

  @Test
  void everyTopicGetsOnlyItsOwnPartitions() {
    // a wide fan-out: many topics, many partitions each - a per-topic scan of the whole
    // cluster map would be O(topics * partitions) here
    int topicCount = 200;
    int partitionsPerTopic = 20;
    var latest = new java.util.HashMap<TopicPartition, Long>();
    for (int t = 0; t < topicCount; t++) {
      for (int p = 0; p < partitionsPerTopic; p++) {
        latest.put(new TopicPartition("topic-" + t, p), (long) (t * 1000 + p));
      }
    }

    var descriptions = new java.util.HashMap<String, TopicDescription>();
    for (int t = 0; t < topicCount; t++) {
      descriptions.put("topic-" + t, new TopicDescription("topic-" + t, false, List.of()));
    }

    var states = ScrapedClusterState.topicStateMap(
        InternalLogDirStats.empty(),
        descriptions,
        Map.of(),
        latest,
        Map.of()
    );

    assertThat(states).hasSize(topicCount);
    for (int t = 0; t < topicCount; t++) {
      var name = "topic-" + t;
      assertThat(states.get(name).endOffsets()).hasSize(partitionsPerTopic);
      assertThat(states.get(name).endOffsets().get(0)).isEqualTo((long) (t * 1000));
      assertThat(states.get(name).endOffsets().get(partitionsPerTopic - 1))
          .isEqualTo((long) (t * 1000 + partitionsPerTopic - 1));
    }
  }
}
