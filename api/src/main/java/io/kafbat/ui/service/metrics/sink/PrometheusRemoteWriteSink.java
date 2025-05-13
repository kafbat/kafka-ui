package io.kafbat.ui.service.metrics.sink;


import io.kafbat.ui.config.ClustersProperties.TruststoreConfig;
import io.kafbat.ui.service.metrics.prometheus.PrometheusExpose;
import io.kafbat.ui.util.MetricsUtils;
import io.kafbat.ui.util.WebClientConfigurator;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import jakarta.annotation.Nullable;
import java.net.URI;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import org.xerial.snappy.Snappy;
import prometheus.Remote;
import prometheus.Types;
import reactor.core.publisher.Mono;

class PrometheusRemoteWriteSink implements MetricsSink {

  private final URI writeEndpoint;
  private final WebClient webClient;

  PrometheusRemoteWriteSink(String prometheusUrl, @Nullable TruststoreConfig truststoreConfig) {
    this.writeEndpoint = URI.create(prometheusUrl).resolve("/api/v1/write");
    this.webClient = new WebClientConfigurator()
        .configureSsl(truststoreConfig, null)
        .configureBufferSize(DataSize.ofMegabytes(20))
        .build();
  }

  @SneakyThrows
  @Override
  public Mono<Void> send(Stream<MetricSnapshot> metrics) {
    byte[] bytesToWrite = Snappy.compress(createWriteRequest(metrics).toByteArray());
    return webClient.post()
        .uri(writeEndpoint)
        .header("Content-Type", "application/x-protobuf")
        .header("User-Agent", "promremote-kafbat-ui/0.1.0")
        .header("Content-Encoding", "snappy")
        .header("X-Prometheus-Remote-Write-Version", "0.1.0")
        .bodyValue(bytesToWrite)
        .retrieve()
        .toBodilessEntity()
        .then();
  }

  private static Remote.WriteRequest createWriteRequest(Stream<MetricSnapshot> metrics) {
    long currentTs = System.currentTimeMillis();
    Remote.WriteRequest.Builder request = Remote.WriteRequest.newBuilder();
    metrics.forEach(mfs -> {
      for (DataPointSnapshot dataPoint : mfs.getDataPoints()) {
        Types.TimeSeries.Builder timeSeriesBuilder = Types.TimeSeries.newBuilder();
        timeSeriesBuilder.addLabels(
            Types.Label.newBuilder().setName("__name__").setValue(mfs.getMetadata().getName())
        );
        for (Label label : dataPoint.getLabels()) {
          timeSeriesBuilder.addLabels(
              Types.Label.newBuilder()
                  .setName(label.getName())
                  .setValue(PrometheusExpose.escapedLabelValue(label.getValue()))
          );
        }
        timeSeriesBuilder.addSamples(
            Types.Sample.newBuilder()
                .setValue(MetricsUtils.readPointValue(dataPoint))
                .setTimestamp(currentTs)
        );
        request.addTimeseries(timeSeriesBuilder);
      }
    });
    //TODO: pass Metadata
    return request.build();
  }


}
