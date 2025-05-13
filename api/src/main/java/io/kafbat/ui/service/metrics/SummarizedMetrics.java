package io.kafbat.ui.service.metrics;

import static io.prometheus.metrics.model.snapshots.CounterSnapshot.*;
import static io.prometheus.metrics.model.snapshots.GaugeSnapshot.*;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.Streams;
import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SummarizedMetrics {

  private final Metrics metrics;

  public Stream<MetricSnapshot> asStream() {
    return Streams.concat(
        metrics.getInferredMetrics().asStream(),
        metrics.getPerBrokerScrapedMetrics()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(
                toMap(
                  mfs -> mfs.getMetadata().getName(),
                  Optional::of, SummarizedMetrics::summarizeMetricSnapshot, LinkedHashMap::new
                )
            ).values()
            .stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
    );
  }

  //returns Optional.empty if merging not supported for metric type
  @SuppressWarnings("unchecked")
  private static Optional<MetricSnapshot> summarizeMetricSnapshot(Optional<MetricSnapshot> mfs1opt,
                                                                  Optional<MetricSnapshot> mfs2opt) {

    if ((mfs1opt.isEmpty() || mfs2opt.isEmpty()) || !(mfs1opt.get().getClass().equals(mfs2opt.get().getClass()))) {
      return Optional.empty();
    }

    var mfs1 = mfs1opt.get();

    if (mfs1 instanceof GaugeSnapshot || mfs1 instanceof CounterSnapshot) {
      BiFunction<Labels, Double, DataPointSnapshot> pointFactory;
      Function<DataPointSnapshot, Double> valueGetter;
      Function<Collection<?>, MetricSnapshot> builder;

      if (mfs1 instanceof CounterSnapshot) {
        pointFactory = (l, v) -> CounterDataPointSnapshot.builder()
            .labels(l)
            .value(v)
            .build();
        valueGetter = (dp) -> ((CounterDataPointSnapshot)dp).getValue();
        builder = (dps) ->
            new CounterSnapshot(mfs1.getMetadata(), (Collection<CounterDataPointSnapshot>)dps);
      } else {
        pointFactory = (l,v) -> GaugeDataPointSnapshot.builder()
            .labels(l)
            .value(v)
            .build();
        valueGetter = (dp) -> ((GaugeDataPointSnapshot)dp).getValue();
        builder = (dps) ->
            new GaugeSnapshot(mfs1.getMetadata(), (Collection<GaugeDataPointSnapshot>)dps);
      }

      Collection<DataPointSnapshot> points =
          Stream.concat(mfs1.getDataPoints().stream(), mfs2opt.get().getDataPoints().stream())
              .collect(
                  toMap(
                      // merging samples with same labels
                      DataPointSnapshot::getLabels,
                      s -> s,
                      (s1, s2) -> pointFactory.apply(
                          s1.getLabels(),
                          valueGetter.apply(s1) + valueGetter.apply(s2)
                      ),
                      LinkedHashMap::new
                  )
              ).values();
      return Optional.of(builder.apply(points));
    } else {
      return Optional.empty();
    }
  }


}
