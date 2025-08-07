package io.kafbat.ui.service.metrics.scrape.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.container.PrometheusContainer;
import io.kafbat.ui.model.MetricsScrapeProperties;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PrometheusMetricsRetrieverTest {

  @Container
  private static final PrometheusContainer PROMETHEUS = new PrometheusContainer();

  @Test
  void testPrometheusMetricsParsedFromEndpoint() {
    List<MetricSnapshot> retrieved = new PrometheusMetricsRetriever(
        MetricsScrapeProperties.builder()
            .port(PROMETHEUS.getMappedPort(9090))
            .ssl(false)
            .build()
    ).retrieve(PROMETHEUS.getHost()).block();

    assertThat(retrieved)
        .map(m -> m.getMetadata().getName())
        .containsAll(
            List.of(
                "go_gc_cycles_automatic_gc_cycles", //counter
                "go_gc_duration_seconds", //histogram
                "go_gc_gogc_percent" //gauge
            )
        );
  }

}
