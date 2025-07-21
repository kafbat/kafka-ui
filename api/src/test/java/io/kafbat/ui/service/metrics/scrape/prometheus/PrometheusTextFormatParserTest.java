package io.kafbat.ui.service.metrics.scrape.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.io.ByteArrayOutputStream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class PrometheusTextFormatParserTest {

  @Test
  void testCounter() {
    String source = """
        # HELP kafka_network_requestmetrics_requests_total Total number of network requests
        # TYPE kafka_network_requestmetrics_requests_total counter
        kafka_network_requestmetrics_requests_total{request="FetchConsumer"} 138912.0
        kafka_network_requestmetrics_requests_total{request="Metadata"} 21001.0
        kafka_network_requestmetrics_requests_total{request="Produce"} 140321.0
        """;
    test2waySerialization(source);
  }

  @Test
  void testGauge() {
    String source = """
        # HELP kafka_controller_kafkacontroller_activecontrollercount Number of active controllers
        # TYPE kafka_controller_kafkacontroller_activecontrollercount gauge
        kafka_controller_kafkacontroller_activecontrollercount 1.0
        """;
    test2waySerialization(source);
  }

  @Test
  void testHistogram() {
    String source = """
        # HELP http_request_duration_seconds Request duration in seconds
        # TYPE http_request_duration_seconds histogram
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="0.01"} 2
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="0.05"} 10
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="0.1"} 32
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="0.25"} 76
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="0.5"} 91
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="1.0"} 98
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="2.5"} 100
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="5.0"} 100
        http_request_duration_seconds_bucket{method="GET",path="/hello",le="+Inf"} 100
        http_request_duration_seconds_count{method="GET",path="/hello"} 100
        http_request_duration_seconds_sum{method="GET",path="/hello"} 22.57
        """;
    test2waySerialization(source);
  }

  @Test
  void testSummary() {
    String source = """
        # HELP kafka_network_requestmetrics_queue_time_ms Total time spent in request queue
        # TYPE kafka_network_requestmetrics_queue_time_ms summary
        kafka_network_requestmetrics_queue_time_ms{request="FetchConsumer",quantile="0.5"} 1.23
        kafka_network_requestmetrics_queue_time_ms{request="FetchConsumer",quantile="0.95"} 5.34
        kafka_network_requestmetrics_queue_time_ms{request="FetchConsumer",quantile="0.99"} 9.12
        kafka_network_requestmetrics_queue_time_ms_count{request="FetchConsumer"} 138912
        kafka_network_requestmetrics_queue_time_ms_sum{request="FetchConsumer"} 37812.3
        """;
    test2waySerialization(source);
  }

  @Test
  void testUntyped() {
    String source = """
        kafka_server_some_untyped_metric{topic="orders"} 138922
        """;
    String expected = """
        # TYPE kafka_server_some_untyped_metric untyped
        kafka_server_some_untyped_metric{topic="orders"} 138922.0
        """;
    test2waySerialization(source, expected);
  }

  @Test
  @SuppressWarnings("checkstyle:LineLength")
  void testVariousTypes() {
    String source = """
        # HELP kafka_server_brokertopicmetrics_totalfetchrequests_total Total number of fetch requests
        # TYPE kafka_server_brokertopicmetrics_totalfetchrequests_total counter
        kafka_server_brokertopicmetrics_totalfetchrequests_total{topic="orders"} 138922.0

        # some invalid comment here
        kafka_server_some_untyped_metric{topic="orders"} 138922

        # Minimalistic line:
        metric_without_timestamp_and_labels 12.47

        # HELP help_no_type Some metric with help, but no type
        help_no_type{lbl="test1"} 1
        help_no_type{lbl="test2"} 2

        # Escaping in label values:
        msdos_file_access_time_seconds{path="C:\\\\DIR\\\\FILE.TXT",error="Cannot find file:\\n\\"FILE.TXT\\""} 1.458255915e9

        # HELP kafka_controller_kafkacontroller_activecontrollercount Number of active controllers
        # TYPE kafka_controller_kafkacontroller_activecontrollercount gauge
        kafka_controller_kafkacontroller_activecontrollercount 1
        """;

    String expected = """
        # HELP help_no_type Some metric with help, but no type
        # TYPE help_no_type untyped
        help_no_type{lbl="test1"} 1.0
        help_no_type{lbl="test2"} 2.0
        # HELP kafka_controller_kafkacontroller_activecontrollercount Number of active controllers
        # TYPE kafka_controller_kafkacontroller_activecontrollercount gauge
        kafka_controller_kafkacontroller_activecontrollercount 1.0
        # HELP kafka_server_brokertopicmetrics_totalfetchrequests_total Total number of fetch requests
        # TYPE kafka_server_brokertopicmetrics_totalfetchrequests_total counter
        kafka_server_brokertopicmetrics_totalfetchrequests_total{topic="orders"} 138922.0
        # TYPE kafka_server_some_untyped_metric untyped
        kafka_server_some_untyped_metric{topic="orders"} 138922.0
        # TYPE metric_without_timestamp_and_labels untyped
        metric_without_timestamp_and_labels 12.47
        # TYPE msdos_file_access_time_seconds untyped
        msdos_file_access_time_seconds{error="Cannot find file:\\n\\"FILE.TXT\\"",path="C:\\\\DIR\\\\FILE.TXT"} 1.458255915E9
        """;

    test2waySerialization(source, expected);
  }

  private void test2waySerialization(String test) {
    test2waySerialization(test, test);
  }

  @SneakyThrows
  private void test2waySerialization(String source,
                                     String expected) {
    var baos = new ByteArrayOutputStream();
    new PrometheusTextFormatWriter(false)
        .write(baos, new MetricSnapshots(new PrometheusTextFormatParser().parse(source)));
    assertThat(baos.toString(Charsets.UTF_8)).isEqualTo(expected);
  }

}
