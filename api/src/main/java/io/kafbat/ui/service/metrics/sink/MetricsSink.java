package io.kafbat.ui.service.metrics.sink;

import static org.springframework.util.StringUtils.hasText;

import io.kafbat.ui.config.ClustersProperties;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MetricsSink {

  static MetricsSink create(ClustersProperties.Cluster cluster) {
    List<MetricsSink> sinks = new ArrayList<>();
    Optional.ofNullable(cluster.getMetrics())
        .flatMap(metrics -> Optional.ofNullable(metrics.getStore()))
        .flatMap(store -> Optional.ofNullable(store.getPrometheus()))
        .ifPresent(prometheusConf -> {
          if (hasText(prometheusConf.getPushGatewayUrl())) {
            sinks.add(
                PrometheusPushGatewaySink.create(
                    prometheusConf.getPushGatewayUrl(),
                    prometheusConf.getPushGatewayUsername(),
                    prometheusConf.getPushGatewayPassword()
                ));
          }
        });
    return compoundSink(sinks);
  }

  private static MetricsSink compoundSink(List<MetricsSink> sinks) {
    return metricsFlux ->
        Flux.fromIterable(sinks)
            .flatMap(sink -> sink.send(metricsFlux))
            .then();
  }

  Mono<Void> send(Flux<MetricSnapshot> metrics);

}
