package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
    Map<String, String[]> gaugeLabels = new HashMap<>();
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
      var gauge = map.computeIfAbsent(
          sanitizedName, n -> {
            gaugeLabels.put(n, lbls);
            return Gauge.builder().name(n).help(n).labelNames(lbls).build();
          }
      );
      if (Arrays.equals(lbls, gaugeLabels.get(sanitizedName))) {
        //using labels of first registered gauge, if not fit - skipping
        gauge.labelValues(lblVals).set(m.value().doubleValue());
      }
    }
    return map.values().stream().map(Gauge::collect);
  }

  record SimpleMetric(String name, Map<String, String> labels, BigDecimal value) implements RawMetric {
  }

}
