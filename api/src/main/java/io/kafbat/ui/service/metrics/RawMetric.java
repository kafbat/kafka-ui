package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public interface RawMetric {

  String name();

  Map<String, String> labels();

  BigDecimal value();

  //--------------------------------------------------

  static RawMetric create(String name, Map<String, String> labels, BigDecimal value) {
    return new SimpleMetric(name, labels, value);
  }

  static Stream<MetricSnapshot> groupIntoSnapshot(Collection<RawMetric> rawMetrics) {
    Map<String, Gauge> map = new LinkedHashMap<>();
    for (RawMetric m : rawMetrics) {
      var lbls = m.labels().keySet()
          .stream()
          .map(PrometheusNaming::sanitizeLabelName)
          .toArray(String[]::new);
      var lblVals = m.labels().keySet()
          .stream()
          .map(l -> m.labels().get(l))
          .toArray(String[]::new);
      var sanitizedName = PrometheusNaming.sanitizeMetricName(m.name());
      // Use composite key (name + label schema) so metrics with the same name
      // but different label sets (e.g. broker-level vs topic-level) each get their own Gauge
      var mapKey = sanitizedName + ":" + String.join(",", lbls);
      var gauge = map.computeIfAbsent(
          mapKey, k -> Gauge.builder().name(sanitizedName).help(sanitizedName).labelNames(lbls).build()
      );
      gauge.labelValues(lblVals).set(m.value().doubleValue());
    }
    return map.values().stream().map(Gauge::collect);
  }

  record SimpleMetric(String name, Map<String, String> labels, BigDecimal value) implements RawMetric {
  }

}
