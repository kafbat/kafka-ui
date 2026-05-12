package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import java.util.List;
import java.util.UUID;
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
