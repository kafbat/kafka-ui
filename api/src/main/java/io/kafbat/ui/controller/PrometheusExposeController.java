package io.kafbat.ui.controller;

import io.kafbat.ui.api.PrometheusExposeApi;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.StatisticsCache;
import io.kafbat.ui.service.metrics.prometheus.PrometheusMetricsExposer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PrometheusExposeController extends AbstractController implements PrometheusExposeApi {

  private final StatisticsCache statisticsCache;

  @Override
  public Mono<ResponseEntity<Void>> exposeAllMetrics(ServerWebExchange exchange) {
    String metrics = PrometheusMetricsExposer.exposeAllMetrics(
        clustersStorage.getKafkaClusters()
            .stream()
            .filter(KafkaCluster::isExposeMetricsViaPrometheusEndpoint)
            .collect(Collectors.toMap(
                KafkaCluster::getName,
                c -> statisticsCache.get(c).getMetrics()))
    ).getBody();

    var response = exchange.getResponse();
    response.getHeaders().add("Content-Type", "text/plain; version=0.0.4");

    byte[] bytes = metrics.getBytes(StandardCharsets.UTF_8);
    var buffer = response.bufferFactory().wrap(bytes);

    return response.writeWith(Mono.just(buffer))
        .doOnTerminate(response::setComplete)
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> exposeClusterMetrics(String clusterName, ServerWebExchange exchange) {
    Optional<KafkaCluster> cluster = clustersStorage.getClusterByName(clusterName);
    if (cluster.isEmpty() || !cluster.get().isExposeMetricsViaPrometheusEndpoint()) {
      exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
      return exchange.getResponse().setComplete()
          .thenReturn(ResponseEntity.notFound().build());
    }

    String metrics = PrometheusMetricsExposer.exposeAllMetrics(
        Map.of(clusterName, statisticsCache.get(cluster.get()).getMetrics())
    ).getBody();

    var response = exchange.getResponse();
    response.getHeaders().add("Content-Type", "text/plain; version=0.0.4");

    byte[] bytes = metrics.getBytes(StandardCharsets.UTF_8);
    var buffer = response.bufferFactory().wrap(bytes);

    return response.writeWith(Mono.just(buffer))
        .doOnTerminate(response::setComplete)
        .thenReturn(ResponseEntity.ok().build());
  }
}
