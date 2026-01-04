package io.kafbat.ui.service.metrics.prometheus;


import static io.kafbat.ui.util.MetricsUtils.appendLabel;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.google.common.annotations.VisibleForTesting;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.util.MetricsUtils;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

public final class PrometheusMetricsExposer {

  private static final String CLUSTER_EXPOSE_LBL_NAME = "cluster";
  private static final String BROKER_EXPOSE_LBL_NAME = "broker_id";

  private static final HttpHeaders PROMETHEUS_EXPOSE_ENDPOINT_HEADERS;

  static {
    PROMETHEUS_EXPOSE_ENDPOINT_HEADERS = new HttpHeaders();
    PROMETHEUS_EXPOSE_ENDPOINT_HEADERS.set(CONTENT_TYPE, PrometheusTextFormatWriter.CONTENT_TYPE);
  }

  private PrometheusMetricsExposer() {
  }

  public static ResponseEntity<String> exposeAllMetrics(Map<String, Metrics> clustersMetrics) {
    return constructHttpsResponse(getMetricsForGlobalExpose(clustersMetrics));
  }

  private static MetricSnapshots getMetricsForGlobalExpose(Map<String, Metrics> clustersMetrics) {
    return new MetricSnapshots(clustersMetrics.entrySet()
        .stream()
        .flatMap(e -> prepareMetricsForGlobalExpose(e.getKey(), e.getValue()))
        // merging MFS with same name with LinkedHashMap(for order keeping)
        .collect(Collectors.toMap(mfs -> mfs.getMetadata().getName(), mfs -> mfs,
            MetricsUtils::concatDataPoints, LinkedHashMap::new))
        .values());
  }

  public static Stream<MetricSnapshot> prepareMetricsForGlobalExpose(String clusterName, Metrics metrics) {
    return Stream.concat(
            metrics.getInferredMetrics().asStream(),
            extractBrokerMetricsWithLabel(metrics)
        )
        .map(mfs -> appendLabel(mfs, CLUSTER_EXPOSE_LBL_NAME, clusterName));
  }

  private static Stream<MetricSnapshot> extractBrokerMetricsWithLabel(Metrics metrics) {
    return metrics.getPerBrokerScrapedMetrics().entrySet().stream()
        .flatMap(e -> {
          String brokerId = String.valueOf(e.getKey());
          return e.getValue().stream().map(mfs -> appendLabel(mfs, BROKER_EXPOSE_LBL_NAME, brokerId));
        });
  }

  @VisibleForTesting
  @SneakyThrows
  public static ResponseEntity<String> constructHttpsResponse(MetricSnapshots metrics) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PrometheusTextFormatWriter writer = new PrometheusTextFormatWriter(false);
    writer.write(buffer, metrics);
    return ResponseEntity
        .ok()
        .headers(PROMETHEUS_EXPOSE_ENDPOINT_HEADERS)
        .body(buffer.toString(StandardCharsets.UTF_8));
  }
}
