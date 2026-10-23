package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.service.metrics.scrape.ScrapedClusterState.TopicConfigsRefreshPlan;
import static io.kafbat.ui.service.metrics.scrape.ScrapedClusterState.TopicState;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.service.index.FilterTopicIndex;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
    try (ScrapedClusterState previous = state(Instant.now(), topicStates("t1", List.of(RETENTION_1H)))) {

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

  @Test
  void planDescribesEveryTopicOnFirstScrape() throws Exception {
    try (ScrapedClusterState previous = ScrapedClusterState.empty()) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1", "t2"), Duration.ofHours(1), Instant.now());
      assertThat(plan.fullRefresh()).isTrue();
      assertThat(plan.topicsToDescribe()).containsExactlyInAnyOrder("t1", "t2");
    }
  }

  @Test
  void planDescribesNothingWhenExpiryNotElapsedAndNoNewTopics() throws Exception {
    Instant now = Instant.now();
    try (ScrapedClusterState previous = state(now.minus(Duration.ofMinutes(1)),
        topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1"), Duration.ofHours(1), now);
      assertThat(plan.fullRefresh()).isFalse();
      assertThat(plan.topicsToDescribe()).isEmpty();
    }
  }

  @Test
  void planDescribesOnlyNewTopicsWhenExpiryNotElapsed() throws Exception {
    Instant now = Instant.now();
    try (ScrapedClusterState previous = state(now.minus(Duration.ofMinutes(1)),
        topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1", "t2"), Duration.ofHours(1), now);
      assertThat(plan.fullRefresh()).isFalse();
      assertThat(plan.topicsToDescribe()).containsExactly("t2");
    }
  }

  @Test
  void planDoesNotAdvanceRefreshClockForIncrementalFetch() throws Exception {
    Instant now = Instant.now();
    Instant refreshedAt = now.minus(Duration.ofMinutes(1));
    try (ScrapedClusterState previous = state(refreshedAt, topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1", "t2"), Duration.ofHours(1), now);
      // steady topic churn must not keep postponing the full refresh
      assertThat(plan.refreshedAt(refreshedAt, now)).isEqualTo(refreshedAt);
    }
  }

  @Test
  void planAdvancesRefreshClockOnFullRefresh() throws Exception {
    Instant now = Instant.now();
    Instant refreshedAt = now.minus(Duration.ofHours(2));
    try (ScrapedClusterState previous = state(refreshedAt, topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1"), Duration.ofHours(1), now);
      assertThat(plan.fullRefresh()).isTrue();
      assertThat(plan.refreshedAt(refreshedAt, now)).isEqualTo(now);
    }
  }

  @Test
  void planDescribesEveryTopicWhenExpiryElapsed() throws Exception {
    Instant now = Instant.now();
    try (ScrapedClusterState previous = state(now.minus(Duration.ofHours(2)),
        topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1"), Duration.ofHours(1), now);
      assertThat(plan.fullRefresh()).isTrue();
      assertThat(plan.topicsToDescribe()).containsExactly("t1");
    }
  }

  @ParameterizedTest
  @MethodSource("nonPositiveExpiries")
  void planDescribesEveryTopicWhenExpiryIsNotPositive(Duration expiry) throws Exception {
    Instant now = Instant.now();
    try (ScrapedClusterState previous = state(now, topicStates("t1", List.of(RETENTION_1H)))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1"), expiry, now);
      assertThat(plan.fullRefresh()).isTrue();
      assertThat(plan.topicsToDescribe()).containsExactly("t1");
    }
  }

  static Stream<Duration> nonPositiveExpiries() {
    return Stream.of(Duration.ZERO, Duration.ofMinutes(-5), null);
  }

  @Test
  void planDoesNotReDescribeTopicsWhoseConfigsAreDenied() throws Exception {
    Instant now = Instant.now();
    // a DESCRIBE_CONFIGS-denied topic is present in topicStates but with empty configs: it must not be
    // re-described every scrape, which is what keying the plan on config content would do
    try (ScrapedClusterState previous = state(now.minus(Duration.ofMinutes(1)), topicStates("t1", List.of()))) {
      var plan = TopicConfigsRefreshPlan.plan(previous, Set.of("t1"), Duration.ofHours(1), now);
      assertThat(plan.topicsToDescribe()).isEmpty();
    }
  }

  private static ScrapedClusterState state(Instant topicConfigsRefreshedAt, Map<String, TopicState> topicStates) {
    return ScrapedClusterState.builder()
        .scrapeFinishedAt(Instant.now())
        .topicConfigsRefreshedAt(topicConfigsRefreshedAt)
        .nodesStates(Map.of())
        .topicStates(topicStates)
        .consumerGroupsStates(Map.of())
        .topicIndex(new FilterTopicIndex(List.of()))
        .build();
  }

  private static Map<String, TopicState> topicStates(String topic, List<ConfigEntry> configs) {
    return Map.of(topic, new TopicState(topic, description(topic), configs, Map.of(), Map.of(), null, null));
  }

  private static TopicDescription description(String topic) {
    return new TopicDescription(topic, false, List.of(new TopicPartitionInfo(0, NODE, List.of(NODE), List.of(NODE))));
  }
}
