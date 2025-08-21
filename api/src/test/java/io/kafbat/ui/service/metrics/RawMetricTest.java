package io.kafbat.ui.service.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RawMetricTest {

  @Test
  void groupIntoSnapshotChoosesFirstGaugeLabels() {
    List<MetricSnapshot> list = RawMetric.groupIntoSnapshot(
        List.of(
            RawMetric.create("name", Map.of("l1", "v1"), BigDecimal.ONE),
            RawMetric.create("name", Map.of("l1", "v11"), BigDecimal.TWO),
            RawMetric.create("name", Map.of("l1", "v1", "l2", "v2"), BigDecimal.TEN)
        )
    ).toList();

    assertThat(list)
        .hasSize(1)
        .element(0)
        .satisfies(snap -> {
              assertThat(snap.getDataPoints())
                  .map(DataPointSnapshot::getLabels)
                  .containsExactly(Labels.of("l1", "v1"), Labels.of("l1", "v11"));
            }
        );
  }

}
