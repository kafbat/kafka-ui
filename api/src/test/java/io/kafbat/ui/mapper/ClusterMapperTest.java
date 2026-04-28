package io.kafbat.ui.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.BrokerMetricsDTO;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClusterMapperTest {

  // Reproduces https://github.com/kafbat/kafka-ui/issues/1630 — JMX/Prometheus exporters
  // emit NaN for unused gauges (e.g. average latency on a never-used listener). Without
  // a guard, BigDecimal.valueOf(NaN) throws NumberFormatException and the whole broker
  // metrics response is silently turned into 404 by BrokersController.
  @Test
  void toBrokerMetricsDropsNonFiniteDataPoints() {
    ClusterMapper mapper = new ClusterMapperImpl();

    var withNan = new GaugeSnapshot(new MetricMetadata("nan_metric", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(Double.NaN, Labels.of("l", "v"), null)));
    var withPosInf = new GaugeSnapshot(new MetricMetadata("posinf_metric", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(Double.POSITIVE_INFINITY, Labels.of("l", "v"), null)));
    var withNegInf = new GaugeSnapshot(new MetricMetadata("neginf_metric", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(Double.NEGATIVE_INFINITY, Labels.of("l", "v"), null)));
    var finite = new GaugeSnapshot(new MetricMetadata("ok_metric", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(42.0, Labels.of("l", "v"), null)));

    BrokerMetricsDTO dto = mapper.toBrokerMetrics(
        List.<MetricSnapshot>of(withNan, withPosInf, withNegInf, finite));

    assertThat(dto.getMetrics())
        .extracting(m -> m.getName())
        .containsExactly("ok_metric");
  }
}
