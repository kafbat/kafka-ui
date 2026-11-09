package io.kafbat.ui.service;

import static io.kafbat.ui.api.model.ControllerType.KRAFT;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StatisticsServiceTest extends AbstractIntegrationTest {

  @Autowired
  private MessagesService messagesService;

  @Autowired
  private ClustersStorage clustersStorage;

  @Autowired
  private StatisticsService statisticsService;

  @Autowired
  private ClustersProperties clustersProperties;

  @Test
  void testInferredMetricsCollected() {
    var newTopicName = "interred_metrics_" + UUID.randomUUID();
    createTopic(new NewTopic(newTopicName, 2, (short) 1));
    for (int i = 0; i < 4; i++) {
      messagesService.sendMessage(
          clustersStorage.getClusterByName(LOCAL).get(),
          newTopicName,
          new CreateTopicMessageDTO()
              .key(UUID.randomUUID().toString())
              .value(UUID.randomUUID().toString())
              .partition(0)
              .keySerde("String")
              .valueSerde("String")
      ).block();
    }

    Statistics updated =
        statisticsService.updateCache(clustersStorage.getClusterByName(LOCAL).get())
            .block();

    assertThat(updated.getController()).isEqualTo(KRAFT);
    assertThat(updated.getQuorumInfo()).isNotNull();

    var kafkaTopicPartitionsGauge = getGaugeSnapshot(
        updated.getMetrics().getInferredMetrics(),
        "kafka_topic_partitions",
        Labels.of("topic", newTopicName)
    );
    assertThat(kafkaTopicPartitionsGauge.getValue())
        .isEqualTo(2);

    var kafkaTopicPartitionNextOffset = getGaugeSnapshot(
        updated.getMetrics().getInferredMetrics(),
        "kafka_topic_partition_next_offset",
        Labels.of("topic", newTopicName, "partition", "0")
    );
    assertThat(kafkaTopicPartitionNextOffset.getValue())
        .isEqualTo(4);
  }

  @Test
  void topicConfigsSurviveSecondScrapeWithinExpiry() {
    var cluster = clustersStorage.getClusterByName(LOCAL).get();
    var topic = "configs_carried_forward_" + UUID.randomUUID();
    createTopic(new NewTopic(topic, 1, (short) 1));

    withTopicConfigsExpiry(Duration.ofHours(1), () -> {
      Statistics first = statisticsService.updateCache(cluster).block();
      Instant firstRefreshedAt = first.getClusterState().getTopicConfigsRefreshedAt();
      assertThat(configsOf(first, topic)).isNotEmpty();

      Statistics second = statisticsService.updateCache(cluster).block();

      // the expiry has not elapsed, so nothing was re-described and the refresh window did not restart
      assertThat(second.getClusterState().getTopicConfigsRefreshedAt()).isEqualTo(firstRefreshedAt);
      // cleanup.policy is what InternalTopic.cleanUpPolicy is derived from, and messagesCount depends on that,
      // so losing it here would show up as a null message count and a reordered MESSAGES_COUNT sort
      assertThat(configsOf(second, topic))
          .anyMatch(entry -> entry.name().equals("cleanup.policy") && entry.value() != null);
    });
  }

  @Test
  void topicCreatedBetweenScrapesGetsConfigsOnNextScrape() {
    var cluster = clustersStorage.getClusterByName(LOCAL).get();

    withTopicConfigsExpiry(Duration.ofHours(1), () -> {
      Statistics first = statisticsService.updateCache(cluster).block();
      Instant firstRefreshedAt = first.getClusterState().getTopicConfigsRefreshedAt();

      var topic = "configs_for_new_topic_" + UUID.randomUUID();
      createTopic(new NewTopic(topic, 1, (short) 1));

      Statistics second = statisticsService.updateCache(cluster).block();

      // a topic we have not seen before is always described, even inside the expiry window
      assertThat(configsOf(second, topic)).isNotEmpty();
      // ...but an incremental fetch must not restart the window, or topic churn would postpone full refreshes
      assertThat(second.getClusterState().getTopicConfigsRefreshedAt()).isEqualTo(firstRefreshedAt);
    });
  }

  private void withTopicConfigsExpiry(Duration expiry, Runnable body) {
    Duration original = clustersProperties.getScrape().getTopicConfigsExpiry();
    clustersProperties.getScrape().setTopicConfigsExpiry(expiry);
    try {
      body.run();
    } finally {
      clustersProperties.getScrape().setTopicConfigsExpiry(original);
    }
  }

  private static List<ConfigEntry> configsOf(Statistics statistics, String topic) {
    return statistics.getClusterState().getTopicStates().get(topic).configs();
  }

  @SuppressWarnings("unchecked")
  private GaugeDataPointSnapshot getGaugeSnapshot(InferredMetrics inferredMetrics,
                                                  String metricName,
                                                  Labels labels) {
    return inferredMetrics.asStream()
        .filter(s -> s.getMetadata().getName().equals(metricName) && s instanceof GaugeSnapshot)
        .flatMap(s -> ((List<GaugeDataPointSnapshot>) s.getDataPoints()).stream())
        .filter(dp -> dp.getLabels().equals(labels))
        .findFirst()
        .orElseThrow();
  }
}
