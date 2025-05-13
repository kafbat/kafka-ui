package io.kafbat.ui.service.metrics.sink;

import static org.springframework.util.StringUtils.hasText;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
class PrometheusPushGatewaySink implements MetricsSink {

  private final PushGateway pushGateway;

  @SneakyThrows
  static PrometheusPushGatewaySink create(String url,
                                          @Nullable String username,
                                          @Nullable String passw) {
    PushGateway.Builder builder = PushGateway.builder()
        .address(url);


    if (hasText(username) && hasText(passw)) {
      builder.basicAuth(username, passw);
    }
    return new PrometheusPushGatewaySink(builder.build());
  }

  @Override
  public Mono<Void> send(Stream<MetricSnapshot> metrics) {
    List<MetricSnapshot> metricsToPush = metrics.toList();
    if (metricsToPush.isEmpty()) {
      return Mono.empty();
    }
    return Mono.<Void>fromRunnable(() -> pushSync(metricsToPush))
        .subscribeOn(Schedulers.boundedElastic());
  }

  @SneakyThrows
  private void pushSync(List<MetricSnapshot> metricsToPush) {
    pushGateway.push(() -> new MetricSnapshots(metricsToPush));
  }
}
