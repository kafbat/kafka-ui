package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.service.metrics.MetricsUtils.isTheSameMetric;
import static io.kafbat.ui.service.metrics.scrape.prometheus.PrometheusEndpointParser.parse;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.service.metrics.prometheus.PrometheusExpose;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

class PrometheusEndpointParserTest {

  @Test
  void parsesAllGeneratedMetricTypes() {
    MetricSnapshots original = generateMfs();
    String exposed = PrometheusExpose.constructHttpsResponse(original).getBody();
    List<MetricSnapshot> parsed = parse(exposed.lines());
    assertThat(parsed).containsExactlyElementsOf(original);
  }

  @Test
  void parsesMetricsFromPrometheusEndpointOutput() {
    String expose = """
            # HELP http_requests_total The total number of HTTP requests.
            # TYPE http_requests_total counter
            http_requests_total{method="post",code="200",} 1027 1395066363000
            http_requests_total{method="post",code="400",}    3 1395066363000
            # Minimalistic line:
            metric_without_timestamp_and_labels 12.47
            # A weird metric from before the epoch:
            something_weird{problem="division by zero"} +Inf -3982045
            # TYPE something_untyped untyped
            something_untyped{} -123123
            # TYPE unit_test_seconds counter
            # UNIT unit_test_seconds seconds
            # HELP unit_test_seconds Testing that unit parsed properly
            unit_test_seconds_total 4.20072246e+06
            # HELP http_request_duration_seconds A histogram of the request duration.
            # TYPE http_request_duration_seconds histogram
            http_request_duration_seconds_bucket{le="0.05"} 24054
            http_request_duration_seconds_bucket{le="0.1"} 33444
            http_request_duration_seconds_bucket{le="0.2"} 100392
            http_request_duration_seconds_bucket{le="0.5"} 129389
            http_request_duration_seconds_bucket{le="1"} 133988
            http_request_duration_seconds_bucket{le="+Inf"} 144320
            http_request_duration_seconds_sum 53423
            http_request_duration_seconds_count 144320
        """;
    List<MetricSnapshot> parsed = parse(expose.lines());

    assertThat(parsed).anyMatch(p -> isTheSameMetric(p,
        new CounterSnapshot(
            new MetricMetadata("http_requests", "The total number of HTTP requests."),
            List.of(
                new CounterSnapshot.CounterDataPointSnapshot(
                    1027,
                    Labels.of("method", "post", "code", "200"),
                    null,
                    0
                ),
                new CounterSnapshot.CounterDataPointSnapshot(
                    3,
                    Labels.of("method", "post", "code", "400"),
                    null,
                    0
                )
            )
        ))
    ).anyMatch(p -> isTheSameMetric(p,
          new GaugeSnapshot(
              new MetricMetadata("metric_without_timestamp_and_labels", "metric_without_timestamp_and_labels"),
              List.of(
                  new GaugeSnapshot.GaugeDataPointSnapshot(12.47, Labels.EMPTY, null)
              )
          ))
    ).anyMatch(p -> isTheSameMetric(p,
            new GaugeSnapshot(
                new MetricMetadata("something_weird", "something_weird"),
                List.of(
                    new GaugeSnapshot.GaugeDataPointSnapshot(POSITIVE_INFINITY,
                        Labels.of("problem", "division by zero"), null)
                )
            ))

//        new MetricFamilySamples(
//            "something_untyped",
//            Type.GAUGE,
//            "something_untyped",
//            List.of(new Sample("something_untyped", List.of(), List.of(), -123123))
//        ),
//        new MetricFamilySamples(
//            "unit_test_seconds",
//            "seconds",
//            Type.COUNTER,
//            "Testing that unit parsed properly",
//            List.of(new Sample("unit_test_seconds_total", List.of(), List.of(), 4.20072246e+06))
//        ),
//        new MetricFamilySamples(
//            "http_request_duration_seconds",
//            Type.HISTOGRAM,
//            "A histogram of the request duration.",
//            List.of(
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("0.05"), 24054),
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("0.1"), 33444),
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("0.2"), 100392),
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("0.5"), 129389),
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("1"), 133988),
//                new Sample("http_request_duration_seconds_bucket", List.of("le"), List.of("+Inf"), 144320),
//                new Sample("http_request_duration_seconds_sum", List.of(), List.of(), 53423),
//                new Sample("http_request_duration_seconds_count", List.of(), List.of(), 144320)
//            )
//        )
    );
  }

  private MetricSnapshots generateMfs() {
    PrometheusRegistry collectorRegistry = new PrometheusRegistry();

    Gauge.builder()
        .name("test_gauge")
        .help("help for gauge")
        .register(collectorRegistry)
        .set(42);

    Info.builder()
        .name("test_info")
        .help("help for info")
        .labelNames("branch", "version", "revision")
        .register(collectorRegistry)
        .addLabelValues("HEAD", "1.2.3", "e0704b");

    Counter.builder()
        .name("counter_no_labels")
        .help("help for counter no lbls")
        .register(collectorRegistry)
        .inc(111);

    var counterWithLbls = Counter.builder()
        .name("counter_with_labels")
        .help("help for counter with lbls")
        .labelNames("lbl1", "lbl2")
        .register(collectorRegistry);

    counterWithLbls.labelValues("v1", "v2").inc(234);
    counterWithLbls.labelValues("v11", "v22").inc(345);

    var histogram = Histogram.builder()
        .name("test_hist")
        .help("help for hist")
//        .linearBuckets(0.0, 1.0, 10)
        .labelNames("lbl1", "lbl2")
        .register(collectorRegistry);

    var summary = Summary.builder()
        .name("test_summary")
        .help("help for hist")
        .labelNames("lbl1", "lbl2")
        .register(collectorRegistry);

    for (int i = 0; i < 30; i++) {
      var val = ThreadLocalRandom.current().nextDouble(10.0);
      histogram.labelValues("v1", "v2").observe(val);
      summary.labelValues("v1", "v2").observe(val);
    }

//    //emulating unknown type
//    collectorRegistry.register(new Collector() {
//      @Override
//      public List<MetricSnapshot> collect() {
//        return List.of(
//            new MetricFamilySamples(
//                "test_unknown",
//                Type.UNKNOWN,
//                "help for unknown",
//                List.of(new Sample("test_unknown", List.of("l1"), List.of("v1"), 23432.0))
//            )
//        );
//      }
//    });
//    return Lists.newArrayList(Iterators.forEnumeration(collectorRegistry.scrape()));
    return collectorRegistry.scrape();
  }

}
