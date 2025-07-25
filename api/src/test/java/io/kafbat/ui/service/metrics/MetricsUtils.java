package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.Optional;

public class MetricsUtils {

  private MetricsUtils() {
  }

  public static boolean isTheSameMetric(MetricSnapshot m1, MetricSnapshot m2) {
    if (m1.getClass().equals(m2.getClass())) {
      MetricMetadata metadata1 = m1.getMetadata();
      MetricMetadata metadata2 = m2.getMetadata();
      if (
          metadata1.getName().equals(metadata2.getName())
              && metadata1.getHelp().equals(metadata2.getHelp())
              && Optional.ofNullable(
              metadata1.getUnit()).map(u -> u.equals(metadata2.getUnit())
          ).orElse(metadata2.getUnit() == null)
      ) {
        if (m1.getDataPoints().size() == m2.getDataPoints().size()) {
          for (int i = 0; i < m1.getDataPoints().size(); i++) {
            var m1dp = m1.getDataPoints().get(i);
            var m2dp = m2.getDataPoints().get(i);
            boolean same = isTheSameDataPoint(m1dp, m2dp);
            if (!same) {
              return false;
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isTheSameDataPoint(DataPointSnapshot dp1, DataPointSnapshot dp2) {
    if (dp1.getClass().equals(dp2.getClass())) {
      if (Optional.ofNullable(dp1.getLabels()).map(l -> l.equals(dp2.getLabels()))
          .orElse(dp2.getLabels() == null)) {
        if (dp1 instanceof GaugeSnapshot.GaugeDataPointSnapshot g1) {
          GaugeSnapshot.GaugeDataPointSnapshot g2 = (GaugeSnapshot.GaugeDataPointSnapshot) dp2;
          return Double.compare(g1.getValue(), g2.getValue()) == 0;
        }
        if (dp1 instanceof CounterSnapshot.CounterDataPointSnapshot c1) {
          CounterSnapshot.CounterDataPointSnapshot c2 = (CounterSnapshot.CounterDataPointSnapshot) dp2;
          return Double.compare(c1.getValue(), c2.getValue()) == 0;
        }
        return true;
      }
    }
    return false;
  }

}
