package io.kafbat.ui.service.metrics.scrape;

import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.Map;

public record PerBrokerScrapedMetrics(Map<Integer, List<MetricSnapshot>> perBrokerMetrics) {

  static PerBrokerScrapedMetrics empty() {
    return new PerBrokerScrapedMetrics(Map.of());
  }

  Metrics.IoRates ioRates() {
    return new IoRatesMetricsScanner(perBrokerMetrics).get();
  }

}
