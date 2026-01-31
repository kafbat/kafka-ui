package io.kafbat.ui.util;

import static io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import static io.prometheus.metrics.model.snapshots.HistogramSnapshot.HistogramDataPointSnapshot;
import static io.prometheus.metrics.model.snapshots.SummarySnapshot.SummaryDataPointSnapshot;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot.HistogramDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import io.prometheus.metrics.model.snapshots.SummarySnapshot.SummaryDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot.UnknownDataPointSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MetricsUtils {

  private MetricsUtils() {
  }

  public static double readPointValue(DataPointSnapshot dps) {
    if (dps instanceof UnknownDataPointSnapshot) {
      return ((UnknownDataPointSnapshot) dps).getValue();
    } else if (dps instanceof GaugeDataPointSnapshot) {
      return ((GaugeDataPointSnapshot) dps).getValue();
    } else if (dps instanceof CounterDataPointSnapshot) {
      return ((CounterDataPointSnapshot) dps).getValue();
    } else {
      return 0;
    }
  }

  public static MetricSnapshot appendLabel(MetricSnapshot md, String name, String value) {
    if (md instanceof UnknownSnapshot) {
      return new UnknownSnapshot(md.getMetadata(), ((UnknownSnapshot) md).getDataPoints()
          .stream().map(dp ->
              new UnknownDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar(),
                  dp.getScrapeTimestampMillis()
              )
          ).toList()
      );
    } else if (md instanceof GaugeSnapshot) {
      return new GaugeSnapshot(md.getMetadata(), ((GaugeSnapshot) md).getDataPoints()
          .stream().map(dp ->
              new GaugeDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar()
              )
          ).toList());
    } else if (md instanceof CounterSnapshot) {
      return new CounterSnapshot(md.getMetadata(), ((CounterSnapshot) md).getDataPoints()
          .stream().map(dp ->
              new CounterDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar(),
                  dp.getCreatedTimestampMillis(),
                  dp.getScrapeTimestampMillis()
              )
          ).toList());
    } else if (md instanceof HistogramSnapshot) {
      return new HistogramSnapshot(md.getMetadata(), ((HistogramSnapshot) md).getDataPoints()
          .stream().map(dp ->
              new HistogramDataPointSnapshot(
                  dp.getClassicBuckets(),
                  dp.getSum(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplars(),
                  dp.getCreatedTimestampMillis()
              )
          ).toList());
    } else if (md instanceof SummarySnapshot) {
      return new SummarySnapshot(md.getMetadata(), ((SummarySnapshot) md).getDataPoints()
          .stream().map(dp ->
              new SummaryDataPointSnapshot(
                  dp.getCount(),
                  dp.getSum(),
                  dp.getQuantiles(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplars(),
                  dp.getCreatedTimestampMillis()
              )
          ).toList());
    } else {
      return md;
    }
  }

  @SuppressWarnings("unchecked")
  public static MetricSnapshot concatDataPoints(MetricSnapshot d1, MetricSnapshot d2) {
    List<?> dataPoints = Stream.concat(
        d1.getDataPoints().stream(), d2.getDataPoints().stream()
    ).toList();

    if (d1 instanceof UnknownSnapshot) {
      return new UnknownSnapshot(d1.getMetadata(),
          (Collection<UnknownDataPointSnapshot>) dataPoints);
    } else if (d1 instanceof GaugeSnapshot) {
      return new GaugeSnapshot(d1.getMetadata(),
          (Collection<GaugeDataPointSnapshot>) dataPoints);
    } else if (d1 instanceof CounterSnapshot) {
      return new CounterSnapshot(d1.getMetadata(),
          (Collection<CounterDataPointSnapshot>) dataPoints);
    } else if (d1 instanceof HistogramSnapshot) {
      return new HistogramSnapshot(d1.getMetadata(),
          (Collection<HistogramDataPointSnapshot>) dataPoints);
    } else if (d1 instanceof SummarySnapshot) {
      return new SummarySnapshot(d1.getMetadata(),
          (Collection<SummaryDataPointSnapshot>) dataPoints);
    } else {
      return d1;
    }
  }

  private static Labels extendLabels(Labels labels, String name, String value) {
    if (!labels.contains(name)) {
      return labels.add(name, value);
    } else {
      log.warn("Label {} already exists with value {} not updated to {}, skipping", name, labels.get(name), value);
      return labels;
    }
  }
}
