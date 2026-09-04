package io.kafbat.ui.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.exception.NotFoundException;
import io.kafbat.ui.mapper.DescribeLogDirsMapper;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.Statistics;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class BrokerServiceMetricsTest {

  private static final int BROKER_ID = 1;

  private final StatisticsCache statisticsCache = mock(StatisticsCache.class);
  private final KafkaCluster cluster = KafkaCluster.builder().name("local").build();
  private final BrokerService brokerService = new BrokerService(
      statisticsCache,
      mock(AdminClientService.class),
      mock(DescribeLogDirsMapper.class)
  );

  @Test
  void getBrokerMetricsReturnsEmptyListWhenNoMetricsScraped() {
    when(statisticsCache.get(cluster)).thenReturn(statistics(Map.of()));

    StepVerifier.create(brokerService.getBrokerMetrics(cluster, BROKER_ID))
        .expectNext(List.of())
        .verifyComplete();
  }

  @Test
  void getBrokerMetricsReturnsScrapedMetricsWhenPresent() {
    List<MetricSnapshot> scraped = List.of(mock(MetricSnapshot.class));
    when(statisticsCache.get(cluster)).thenReturn(statistics(Map.of(BROKER_ID, scraped)));

    StepVerifier.create(brokerService.getBrokerMetrics(cluster, BROKER_ID))
        .expectNext(scraped)
        .verifyComplete();
  }

  @Test
  void getBrokerMetricsFailsWithNotFoundForUnknownBroker() {
    when(statisticsCache.get(cluster)).thenReturn(statistics(Map.of()));

    StepVerifier.create(brokerService.getBrokerMetrics(cluster, 42))
        .expectError(NotFoundException.class)
        .verify();
  }

  private Statistics statistics(Map<Integer, List<MetricSnapshot>> perBrokerMetrics) {
    Metrics emptyMetrics = Metrics.empty();
    return Statistics.empty().toBuilder()
        .clusterDescription(new ReactiveAdminClient.ClusterDescription(
            null, "clusterId", List.of(new Node(BROKER_ID, "host", 9092)), Set.of()))
        .metrics(Metrics.builder()
            .ioRates(emptyMetrics.getIoRates())
            .inferredMetrics(emptyMetrics.getInferredMetrics())
            .perBrokerScrapedMetrics(perBrokerMetrics)
            .build())
        .build();
  }
}
