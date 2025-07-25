package io.kafbat.ui.service.metrics.scrape.prometheus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.prometheus.metrics.model.snapshots.ClassicHistogramBuckets;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot.HistogramDataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import io.prometheus.metrics.model.snapshots.Quantile;
import io.prometheus.metrics.model.snapshots.Quantiles;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot.UnknownDataPointSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Parses the Prometheus text format into a {@link MetricSnapshots} object.
 * This class is designed to be the functional inverse of
 * {@code io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter}.
 */
public class PrometheusTextFormatParser {

  // Regex to capture metric name, optional labels, value, and optional timestamp.
  // Groups: 1=name, 2=labels (content), 3=value, 4=timestamp
  private static final Pattern METRIC_LINE_PATTERN = Pattern.compile(
      "^([a-zA-Z_:][a-zA-Z0-9_:]*)" +                        // Metric name
      "(?:\\{(?>[^}]*)\\})?" +                               // Optional labels (atomic group)
      "\\s+" +
      "(-?(?:Inf|NaN|(?:\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)))" +  // Metric value
      "(?:\\s+([0-9]+))?$"                                   // Optional timestamp
  );                // Group 4: Optional timestamp


  private static final Pattern HELP_PATTERN =
      Pattern.compile("^# HELP ([a-zA-Z_:][a-zA-Z0-9_:]*) (.*)");
  private static final Pattern TYPE_PATTERN =
      Pattern.compile("^# TYPE ([a-zA-Z_:][a-zA-Z0-9_:]*) (counter|gauge|histogram|summary|untyped)");
  private static final Pattern LABEL_PATTERN =
      Pattern.compile("([a-zA-Z_:][a-zA-Z0-9_:]*)=\"((?:\\\\\"|\\\\\\\\|\\\\n|[^\"])*)\"");
  public static final String QUANTILE_LABEL = "quantile";

  private record ParsedDataPoint(String name, Labels labels, double value, Long scrapedAt) {
  }

  public List<MetricSnapshot> parse(String textFormat) {
    List<MetricSnapshot> snapshots = new ArrayList<>();
    var cxt = new ParsingContext(snapshots);
    textFormat.lines()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .forEach(line -> {
          if (line.startsWith("#")) {
            parseComment(line, cxt);
          } else {
            parseMetricLine(line, cxt);
          }
        });
    cxt.flushAndReset();
    return snapshots;
  }

  private void parseComment(String line, ParsingContext cxt) {
    if (line.startsWith("# HELP")) {
      Matcher m = HELP_PATTERN.matcher(line);
      if (m.matches()) {
        cxt.metricNameAndHelp(
            PrometheusNaming.sanitizeMetricName(m.group(1)),
            m.group(2)
        );
      }
    } else if (line.startsWith("# TYPE")) {
      Matcher m = TYPE_PATTERN.matcher(line);
      if (m.matches()) {
        cxt.metricNameAndType(
            PrometheusNaming.sanitizeMetricName(m.group(1)),
            MetricType.valueOf(m.group(2).toUpperCase())
        );
      }
    }
  }

  private void parseMetricLine(String line, ParsingContext cxt) {
    Matcher m = METRIC_LINE_PATTERN.matcher(line);
    if (m.matches()) {
      String metricName = m.group(1);
      String labelsString = m.group(2);
      String valueString = m.group(3);
      String timestampString = m.group(4);
      cxt.dataPoint(
          new ParsedDataPoint(
              metricName,
              Optional.ofNullable(labelsString).map(this::parseLabels).orElse(Labels.EMPTY),
              parseDouble(valueString),
              Optional.ofNullable(timestampString).map(Long::parseLong).orElse(0L)));
    }
  }

  private Labels parseLabels(String labelsString) {
    Labels.Builder builder = Labels.builder();
    Matcher m = LABEL_PATTERN.matcher(labelsString);
    while (m.find()) {
      builder.label(m.group(1), unescapeLabelValue(m.group(2)));
    }
    return builder.build();
  }

  private String unescapeLabelValue(String value) {
    return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
  }

  private static double parseDouble(String value) {
    return switch (value) {
      case "+Inf" -> Double.POSITIVE_INFINITY;
      case "-Inf" -> Double.NEGATIVE_INFINITY;
      case "NaN" -> Double.NaN;
      default -> Double.parseDouble(value);
    };
  }

  private enum MetricType {
    COUNTER,
    GAUGE,
    UNTYPED,
    HISTOGRAM,
    SUMMARY
  }

  private static class ParsingContext {

    private final List<MetricSnapshot> sink;

    private String currentMetricName;
    private String currentHelp;
    private MetricDataPointsAccumulator dataPoints;

    ParsingContext(List<MetricSnapshot> sink) {
      this.sink = sink;
    }

    private void reset() {
      currentMetricName = null;
      currentHelp = null;
      dataPoints = null;
    }

    void metricNameAndType(String metricName, MetricType metricType) {
      if (!metricName.equals(currentMetricName)) {
        flushAndReset();
      }
      currentMetricName = metricName;
      dataPoints = switch (metricType) {
        case UNTYPED -> new UntypedDataPointsAccumulator();
        case GAUGE -> new GaugeDataPointsAccumulator();
        case COUNTER -> new CounterDataPointsAccumulator(metricName);
        case HISTOGRAM -> new HistogramDataPointsAccumulator(metricName);
        case SUMMARY -> new SummaryDataPointsAccumulator(metricName);
      };
    }

    void metricNameAndHelp(String metricName, String help) {
      if (!metricName.equals(currentMetricName)) {
        flushAndReset();
      }
      currentMetricName = metricName;
      currentHelp = help;
    }

    void dataPoint(ParsedDataPoint parsedDataPoint) {
      if (currentMetricName == null) {
        currentMetricName = PrometheusNaming.sanitizeMetricName(parsedDataPoint.name);
      }
      if (dataPoints == null) {
        dataPoints = new UntypedDataPointsAccumulator();
      }
      if (!dataPoints.add(parsedDataPoint)) {
        flushAndReset();
        dataPoint(parsedDataPoint);
      }
    }

    void flushAndReset() {
      if (dataPoints != null) {
        dataPoints.buildSnapshot(currentMetricName, currentHelp)
            .ifPresent(sink::add);
      }
      reset();
    }
  }

  interface MetricDataPointsAccumulator {
    boolean add(ParsedDataPoint parsedDataPoint);

    Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help);
  }

  static class UntypedDataPointsAccumulator implements MetricDataPointsAccumulator {

    final List<UnknownDataPointSnapshot> dataPoints = new ArrayList<>();
    String name;

    @Override
    public boolean add(ParsedDataPoint dp) {
      if (name == null) {
        name = dp.name;
      } else if (!name.equals(dp.name)) {
        return false;
      }
      dataPoints.add(
          UnknownDataPointSnapshot.builder()
              .labels(dp.labels).value(dp.value).scrapeTimestampMillis(dp.scrapedAt).build());
      return true;
    }

    @Override
    public Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help) {
      if (dataPoints.isEmpty()) {
        return Optional.empty();
      }
      var builder = UnknownSnapshot.builder().name(name).help(help);
      dataPoints.forEach(builder::dataPoint);
      return Optional.of(builder.build());
    }
  }

  static class GaugeDataPointsAccumulator implements MetricDataPointsAccumulator {

    final List<GaugeDataPointSnapshot> dataPoints = new ArrayList<>();

    @Override
    public boolean add(ParsedDataPoint dp) {
      dataPoints.add(
          GaugeDataPointSnapshot.builder()
              .labels(dp.labels).value(dp.value).scrapeTimestampMillis(dp.scrapedAt).build());
      return true;
    }

    @Override
    public Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help) {
      if (dataPoints.isEmpty()) {
        return Optional.empty();
      }
      var builder = GaugeSnapshot.builder().name(name).help(help);
      dataPoints.forEach(builder::dataPoint);
      return Optional.of(builder.build());
    }
  }

  static class CounterDataPointsAccumulator extends UntypedDataPointsAccumulator {

    final List<CounterDataPointSnapshot> counterDataPoints = new ArrayList<>();

    public CounterDataPointsAccumulator(String name) {
      this.name = name;
    }

    @Override
    public boolean add(ParsedDataPoint dp) {
      if (!dp.name.equals(name + "_total")) {
        return false;
      }
      counterDataPoints.add(
          CounterDataPointSnapshot.builder()
              .labels(dp.labels).value(dp.value).scrapeTimestampMillis(dp.scrapedAt).build());
      return true;
    }

    @Override
    public Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help) {
      if (counterDataPoints.isEmpty()) {
        return Optional.empty();
      }
      var builder = CounterSnapshot.builder().name(name).help(help);
      counterDataPoints.forEach(builder::dataPoint);
      return Optional.of(builder.build());
    }
  }

  @RequiredArgsConstructor
  static class HistogramDataPointsAccumulator implements MetricDataPointsAccumulator {

    //contains cumulative(!) counts
    record Bucket(double le, long count) implements Comparable<Bucket> {
      @Override
      public int compareTo(@NotNull Bucket o) {
        return Double.compare(le, o.le);
      }
    }

    final String name;
    final Map<Labels, Double> sums = new HashMap<>();
    final Multimap<Labels, Bucket> buckets = HashMultimap.create();

    @Override
    public boolean add(ParsedDataPoint dp) {
      if (dp.name.equals(name + "_bucket") && dp.labels.contains("le")) {
        var histLbls = rmLabel(dp.labels, "le");
        buckets.put(histLbls, new Bucket(parseDouble(dp.labels.get("le")), (long) dp.value));
        return true;
      }
      if (dp.name.equals(name + "_count")) {
        return true; //skipping counts
      }
      if (dp.name.equals(name + "_sum")) {
        sums.put(dp.labels, dp.value);
        return true;
      }
      return false;
    }

    @Override
    public Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help) {
      if (buckets.isEmpty()) {
        return Optional.empty();
      }
      var builder = HistogramSnapshot.builder().name(name).help(help);
      buckets.asMap().forEach((labels, buckets) -> {
        buckets = buckets.stream().sorted().toList();
        long prevCount = 0;
        var nonCumulativeBuckets = new ArrayList<Bucket>();
        for (Bucket b : buckets) {
          nonCumulativeBuckets.add(new Bucket(b.le, b.count - prevCount));
          prevCount = b.count;
        }
        builder.dataPoint(
            HistogramDataPointSnapshot.builder()
                .labels(labels)
                .classicHistogramBuckets(
                    ClassicHistogramBuckets.of(
                        nonCumulativeBuckets.stream().map(b -> b.le).toList(),
                        nonCumulativeBuckets.stream().map(b -> b.count).toList()
                    )
                )
                .sum(sums.getOrDefault(labels, Double.NaN))
                .build()
        );
      });
      return Optional.of(builder.build());
    }
  }

  @RequiredArgsConstructor
  static class SummaryDataPointsAccumulator implements MetricDataPointsAccumulator {

    final String name;
    final Map<Labels, Double> sums = new HashMap<>();
    final Map<Labels, Long> counts = new HashMap<>();
    final Multimap<Labels, Quantile> quantiles = HashMultimap.create();

    @Override
    public boolean add(ParsedDataPoint dp) {
      if (dp.name.equals(name) && dp.labels.contains(QUANTILE_LABEL)) {
        var histLbls = rmLabel(dp.labels, QUANTILE_LABEL);
        quantiles.put(histLbls, new Quantile(parseDouble(dp.labels.get(QUANTILE_LABEL)), dp.value));
        return true;
      }
      if (dp.name.equals(name + "_count")) {
        counts.put(dp.labels, (long) dp.value);
        return true;
      }
      if (dp.name.equals(name + "_sum")) {
        sums.put(dp.labels, dp.value);
        return true;
      }
      return false;
    }

    @Override
    public Optional<MetricSnapshot> buildSnapshot(String name, @Nullable String help) {
      if (quantiles.isEmpty()) {
        return Optional.empty();
      }
      var builder = SummarySnapshot.builder().name(name).help(help);
      quantiles.asMap().forEach((labels, localQuantiles) -> {
        builder.dataPoint(
            SummarySnapshot.SummaryDataPointSnapshot.builder()
                .labels(labels)
                .quantiles(Quantiles.of(new ArrayList<>(localQuantiles)))
                .sum(sums.getOrDefault(labels, Double.NaN))
                .count(counts.getOrDefault(labels, 0L))
                .build()
        );
      });
      return Optional.of(builder.build());
    }
  }

  private static Labels rmLabel(Labels labels, String labelToExclude) {
    var builder = Labels.builder();
    labels.stream()
        .filter(l -> !l.getName().equals(labelToExclude))
        .forEach(l -> builder.label(l.getName(), l.getValue()));
    return builder.build();
  }
}
