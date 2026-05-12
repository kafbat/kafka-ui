package io.kafbat.ui.util;

import static io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import static io.prometheus.metrics.model.snapshots.HistogramSnapshot.HistogramDataPointSnapshot;
import static io.prometheus.metrics.model.snapshots.SummarySnapshot.SummaryDataPointSnapshot;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
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
    return switch (dps) {
      case UnknownDataPointSnapshot unknown -> unknown.getValue();
      case GaugeDataPointSnapshot guage -> guage.getValue();
      case CounterDataPointSnapshot counter -> counter.getValue();
      default -> 0;
    };
  }

  public static MetricSnapshot appendLabel(MetricSnapshot md, String name, String value) {
    return switch (md) {
      case UnknownSnapshot unknown -> new UnknownSnapshot(unknown.getMetadata(), unknown.getDataPoints()
          .stream().map(dp ->
              new UnknownDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar(),
                  dp.getScrapeTimestampMillis()
              )
          ).toList()
      );
      case GaugeSnapshot gauge -> new GaugeSnapshot(gauge.getMetadata(), gauge.getDataPoints()
          .stream().map(dp ->
              new GaugeDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar()
              )
          ).toList());
      case CounterSnapshot counter -> new CounterSnapshot(counter.getMetadata(), counter.getDataPoints()
          .stream().map(dp ->
              new CounterDataPointSnapshot(
                  dp.getValue(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplar(),
                  dp.getCreatedTimestampMillis(),
                  dp.getScrapeTimestampMillis()
              )
          ).toList());
      case HistogramSnapshot histogram -> new HistogramSnapshot(histogram.getMetadata(), histogram.getDataPoints()
          .stream().map(dp ->
              new HistogramDataPointSnapshot(
                  dp.getClassicBuckets(),
                  dp.getSum(),
                  extendLabels(dp.getLabels(), name, value),
                  dp.getExemplars(),
                  dp.getCreatedTimestampMillis()
              )
          ).toList());
      case SummarySnapshot summary -> new SummarySnapshot(summary.getMetadata(), summary.getDataPoints()
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
      default -> md;
    };
  }

  @SuppressWarnings("unchecked")
  public static MetricSnapshot concatDataPoints(MetricSnapshot d1, MetricSnapshot d2) {
    List<?> dataPoints = Stream.concat(
        d1.getDataPoints().stream(), d2.getDataPoints().stream()
    ).toList();

    return switch (d1) {
      case UnknownSnapshot u -> new UnknownSnapshot(u.getMetadata(),
          (Collection<UnknownDataPointSnapshot>) dataPoints);
      case GaugeSnapshot g -> new GaugeSnapshot(g.getMetadata(),
          (Collection<GaugeDataPointSnapshot>) dataPoints);
      case CounterSnapshot c -> new CounterSnapshot(c.getMetadata(),
          (Collection<CounterDataPointSnapshot>) dataPoints);
      case HistogramSnapshot h -> new HistogramSnapshot(h.getMetadata(),
          (Collection<HistogramDataPointSnapshot>) dataPoints);
      case SummarySnapshot s -> new SummarySnapshot(s.getMetadata(),
          (Collection<SummaryDataPointSnapshot>) dataPoints);
      default -> d1;
    };
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
