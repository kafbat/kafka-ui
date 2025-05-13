package io.kafbat.ui.service.metrics.scrape.inferred;

import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.stream.Stream;

public class InferredMetrics {

  private final List<MetricSnapshot> metrics;

  public static InferredMetrics empty() {
    return new InferredMetrics(List.of());
  }

  public InferredMetrics(List<MetricSnapshot> metrics) {
    this.metrics = metrics;
  }

  public Stream<MetricSnapshot> asStream() {
    return metrics.stream();
  }

}
