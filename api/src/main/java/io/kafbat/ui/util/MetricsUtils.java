package io.kafbat.ui.util;

import static io.prometheus.metrics.model.snapshots.CounterSnapshot.*;
import static io.prometheus.metrics.model.snapshots.HistogramSnapshot.*;
import static io.prometheus.metrics.model.snapshots.SummarySnapshot.*;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.apache.kafka.clients.MetadataSnapshot;

public final class MetricsUtils {
  public static double readPointValue(DataPointSnapshot dps) {
    return switch (dps) {
      case GaugeDataPointSnapshot guage -> guage.getValue();
      case CounterDataPointSnapshot counter -> counter.getValue();
      default -> 0;
    };
  }

  public static String toGoString(double d) {
    if (d == Double.POSITIVE_INFINITY) {
      return "+Inf";
    } else if (d == Double.NEGATIVE_INFINITY) {
      return "-Inf";
    } else {
      return Double.toString(d);
    }
  }

  public static MetricSnapshot appendLabel(MetricSnapshot md, String name, String value) {
    return switch (md) {
      case GaugeSnapshot gauge ->  new GaugeSnapshot(gauge.getMetadata(), gauge.getDataPoints()
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


  public static Labels extendLabels(Labels labels, String name, String value) {
    return labels.add(name, value);
  }
}
