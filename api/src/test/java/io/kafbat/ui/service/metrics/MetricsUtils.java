package io.kafbat.ui.service.metrics;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.Optional;

public class MetricsUtils {

  private MetricsUtils() {
  }

  private static boolean areMetadataEqual(MetricMetadata metadata1, MetricMetadata metadata2) {
    return metadata1.getName().equals(metadata2.getName())
        && metadata1.getHelp().equals(metadata2.getHelp())
        && Optional.ofNullable(metadata1.getUnit())
        .map(u -> u.equals(metadata2.getUnit()))
        .orElse(metadata2.getUnit() == null);
  }

  public static boolean isTheSameMetric(MetricSnapshot m1, MetricSnapshot m2) {
    if (!m1.getClass().equals(m2.getClass())) {
      return false;
    }
    MetricMetadata metadata1 = m1.getMetadata();
    MetricMetadata metadata2 = m2.getMetadata();
    if (!areMetadataEqual(metadata1, metadata2)) {
      return false;
    }
    var dataPoints1 = m1.getDataPoints();
    var dataPoints2 = m2.getDataPoints();
    if (dataPoints1.size() != dataPoints2.size()) {
      return false;
    }
    for (int i = 0; i < dataPoints1.size(); i++) {
      if (!isTheSameDataPoint(dataPoints1.get(i), dataPoints2.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean areLabelsEqual(Labels labels1, Labels labels2) {
    return Optional.ofNullable(labels1)
        .map(l -> l.equals(labels2))
        .orElse(labels2 == null);
  }

  public static boolean isTheSameDataPoint(DataPointSnapshot dp1, DataPointSnapshot dp2) {
    if (!dp1.getClass().equals(dp2.getClass())) {
      return false;
    }
    if (!areLabelsEqual(dp1.getLabels(), dp2.getLabels())) {
      return false;
    }
    if (dp1 instanceof GaugeSnapshot.GaugeDataPointSnapshot gauge1) {
      var gauge2 = (GaugeSnapshot.GaugeDataPointSnapshot) dp2;
      return Double.compare(gauge1.getValue(), gauge2.getValue()) == 0;
    }
    if (dp1 instanceof CounterSnapshot.CounterDataPointSnapshot counter1) {
      var counter2 = (CounterSnapshot.CounterDataPointSnapshot) dp2;
      return Double.compare(counter1.getValue(), counter2.getValue()) == 0;
    }
    return true;
  }

}
