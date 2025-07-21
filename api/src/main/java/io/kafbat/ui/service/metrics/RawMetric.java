package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.Arrays;
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
      var lbls = m.labels().keySet().toArray(String[]::new);
      var lblVals = Arrays.stream(lbls).map(l -> m.labels().get(l)).toArray(String[]::new);
      var gauge = map.computeIfAbsent(
          m.name(),
          n -> Gauge.builder()
              .name(m.name())
              .help(m.name())
              .labelNames(lbls)
              .build()
      );
      gauge.labelValues(lblVals).set(m.value().doubleValue());
    }
    return map.values().stream().map(Gauge::collect);
  }

  record SimpleMetric(String name, Map<String, String> labels, BigDecimal value) implements RawMetric {
  }

}
