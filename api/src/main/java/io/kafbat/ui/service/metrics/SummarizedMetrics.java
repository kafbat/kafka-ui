package io.kafbat.ui.service.metrics;

import static io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import static io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.Streams;
import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot.UnknownDataPointSnapshot;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@Deprecated(forRemoval = true, since = "1.4.0") //used for api backward-compatibility
@RequiredArgsConstructor
public class SummarizedMetrics {

  private final Metrics metrics;

  public Stream<MetricSnapshot> asStream() {
    return Streams.concat(
        metrics.getInferredMetrics().asStream(),
        summarize(
            metrics.getPerBrokerScrapedMetrics()
                .values()
                .stream()
                .flatMap(Collection::stream)
        )
    );
  }

  private Stream<MetricSnapshot> summarize(Stream<MetricSnapshot> snapshots) {
    return snapshots
        .collect(
            toMap(
                mfs -> mfs.getMetadata().getName(),
                Optional::of, SummarizedMetrics::summarizeMetricSnapshot, LinkedHashMap::new
            )
        ).values()
        .stream()
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  //returns Optional.empty if merging not supported for metric type
  @SuppressWarnings("unchecked")
  private static Optional<MetricSnapshot> summarizeMetricSnapshot(Optional<MetricSnapshot> snap1Opt,
                                                                  Optional<MetricSnapshot> snap2Opt) {

    if ((snap1Opt.isEmpty() || snap2Opt.isEmpty()) || !(snap1Opt.get().getClass().equals(snap2Opt.get().getClass()))) {
      return Optional.empty();
    }

    var snap1 = snap1Opt.get();

    if (snap1 instanceof GaugeSnapshot
        || snap1 instanceof CounterSnapshot
        || snap1 instanceof UnknownSnapshot) {

      BiFunction<Labels, Double, DataPointSnapshot> pointFactory;
      Function<DataPointSnapshot, Double> valueGetter;
      Function<Collection<?>, MetricSnapshot> builder;

      if (snap1 instanceof UnknownSnapshot) {
        pointFactory = (l, v) -> UnknownDataPointSnapshot.builder()
            .labels(l)
            .value(v)
            .build();
        valueGetter = (dp) -> ((UnknownDataPointSnapshot) dp).getValue();
        builder = (dps) ->
            new UnknownSnapshot(snap1.getMetadata(), (Collection<UnknownDataPointSnapshot>) dps);
      } else if (snap1 instanceof CounterSnapshot) {
        pointFactory = (l, v) -> CounterDataPointSnapshot.builder()
            .labels(l)
            .value(v)
            .build();
        valueGetter = (dp) -> ((CounterDataPointSnapshot) dp).getValue();
        builder = (dps) ->
            new CounterSnapshot(snap1.getMetadata(), (Collection<CounterDataPointSnapshot>) dps);
      } else {
        pointFactory = (l, v) -> GaugeDataPointSnapshot.builder()
            .labels(l)
            .value(v)
            .build();
        valueGetter = (dp) -> ((GaugeDataPointSnapshot) dp).getValue();
        builder = (dps) ->
            new GaugeSnapshot(snap1.getMetadata(), (Collection<GaugeDataPointSnapshot>) dps);
      }

      Collection<DataPointSnapshot> points =
          Stream.concat(snap1.getDataPoints().stream(), snap2Opt.get().getDataPoints().stream())
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
