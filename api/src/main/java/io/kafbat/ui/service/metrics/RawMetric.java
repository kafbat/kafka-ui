package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
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

  static Stream<MetricSnapshot> groupIntoMfs(Collection<RawMetric> rawMetrics) {
    Map<String, GaugeSnapshot.Builder> map = new LinkedHashMap<>();
    for (RawMetric m : rawMetrics) {
      var gauge = map.computeIfAbsent(m.name(),
          (n) -> GaugeSnapshot.builder()
              .name(m.name())
              .help(m.name())
      );

      List<String> lbls = m.labels().keySet().stream().toList();
      List<String> lblVals = lbls.stream().map(l -> m.labels().get(l)).toList();

      GaugeSnapshot.GaugeDataPointSnapshot point = GaugeSnapshot.GaugeDataPointSnapshot.builder()
          .value(m.value().doubleValue())
          .labels(Labels.of(lbls, lblVals)).build();
      gauge.dataPoint(point);
    }
    return map.values().stream().map(GaugeSnapshot.Builder::build);
  }

  record SimpleMetric(String name, Map<String, String> labels, BigDecimal value) implements RawMetric { }

}
