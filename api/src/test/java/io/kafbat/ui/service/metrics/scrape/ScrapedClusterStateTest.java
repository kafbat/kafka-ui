package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.service.metrics.scrape.ScrapedClusterState.TopicState;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.service.index.FilterTopicIndex;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

class ScrapedClusterStateTest {

  private static final ConfigEntry RETENTION_1H = new ConfigEntry("retention.ms", "3600000");
  private static final ConfigEntry RETENTION_2H = new ConfigEntry("retention.ms", "7200000");
  private static final Node NODE = new Node(1, "node1", 9092);

  @Test
  void emptyStateHasNonNullTopicIndex() throws Exception {
    try (ScrapedClusterState empty = ScrapedClusterState.empty()) {
      assertThat(empty.getTopicIndex()).isNotNull();
      assertThat(empty.getTopicIndex().find(null, null, false, null)).isEmpty();
      assertThat(empty.getTopicIndex().find("search", true, true, null)).isEmpty();
    }
  }

  @Test
  void mergeKeepsPreviousConfigsWhenFetchOmitsKnownTopic() {
    var merged = ScrapedClusterState.mergeTopicConfigs(topicStates("t1", List.of(RETENTION_1H)), Map.of());
    assertThat(merged.get("t1")).containsExactly(RETENTION_1H);
  }

  @Test
  void mergePrefersFreshlyFetchedConfigs() {
    var merged = ScrapedClusterState.mergeTopicConfigs(
        topicStates("t1", List.of(RETENTION_1H)),
        Map.of("t1", List.of(RETENTION_2H))
    );
    assertThat(merged.get("t1")).containsExactly(RETENTION_2H);
  }

  @Test
  void mergeIgnoresEmptyFetchedConfigList() {
    var merged = ScrapedClusterState.mergeTopicConfigs(
        topicStates("t1", List.of(RETENTION_1H)),
        Map.of("t1", List.of())
    );
    assertThat(merged.get("t1")).containsExactly(RETENTION_1H);
  }

  @Test
  void mergeAddsConfigsForTopicsAbsentFromPreviousState() {
    var merged = ScrapedClusterState.mergeTopicConfigs(Map.of(), Map.of("t2", List.of(RETENTION_2H)));
    assertThat(merged.get("t2")).containsExactly(RETENTION_2H);
  }

  @Test
  void mergeSkipsTopicsWithNoConfigsOnEitherSide() {
    var merged = ScrapedClusterState.mergeTopicConfigs(topicStates("t1", List.of()), Map.of());
    assertThat(merged).isEmpty();
  }

  @Test
  void updateTopicsKeepsExistingConfigsWhenConfigMapMissesTopic() throws Exception {
    ClustersProperties properties = new ClustersProperties();
    try (ScrapedClusterState previous = ScrapedClusterState.builder()
        .scrapeFinishedAt(Instant.now())
        .nodesStates(Map.of())
        .topicStates(topicStates("t1", List.of(RETENTION_1H)))
        .consumerGroupsStates(Map.of())
        .topicIndex(new FilterTopicIndex(List.of()))
        .build()) {

      // configs deliberately empty: this is what a swallowed per-topic describeConfigs failure looks like
      try (ScrapedClusterState updated = previous.updateTopics(
          Map.of("t1", description("t1")),
          Map.of(),
          new InternalPartitionsOffsets(Map.of()),
          properties)) {
        assertThat(updated.getTopicStates().get("t1").configs()).containsExactly(RETENTION_1H);
      }
    }
  }

  private static Map<String, TopicState> topicStates(String topic, List<ConfigEntry> configs) {
    return Map.of(topic, new TopicState(topic, description(topic), configs, Map.of(), Map.of(), null, null));
  }

  private static TopicDescription description(String topic) {
    return new TopicDescription(topic, false, List.of(new TopicPartitionInfo(0, NODE, List.of(NODE), List.of(NODE))));
  }
}
