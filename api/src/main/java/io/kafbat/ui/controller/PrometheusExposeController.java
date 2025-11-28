package io.kafbat.ui.controller;

import io.kafbat.ui.api.PrometheusExposeApi;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.StatisticsCache;
import io.kafbat.ui.service.metrics.prometheus.PrometheusMetricsExposer;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PrometheusExposeController extends AbstractController implements PrometheusExposeApi {

  private final StatisticsCache statisticsCache;

  @Override
  public Mono<ResponseEntity<String>> exposeAllMetrics(ServerWebExchange exchange) {

    String metrics = PrometheusMetricsExposer.exposeAllMetrics(
        clustersStorage.getKafkaClusters().stream()
            .filter(KafkaCluster::isExposeMetricsViaPrometheusEndpoint)
            .collect(Collectors.toMap(
                KafkaCluster::getName,
                c -> statisticsCache.get(c).getMetrics()
            ))
    ).getBody();

    return Mono.just(
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/plain; version=0.0.4"))
            .body(metrics)
    );
  }

  @Override
  public Mono<ResponseEntity<String>> exposeClusterMetrics(String clusterName, ServerWebExchange exchange) {

    Optional<KafkaCluster> cluster = clustersStorage.getClusterByName(clusterName);

    if (cluster.isEmpty() || !cluster.get().isExposeMetricsViaPrometheusEndpoint()) {
      return Mono.just(ResponseEntity.notFound().build());
    }

    String metrics = PrometheusMetricsExposer.exposeAllMetrics(
        Map.of(
            clusterName,
            statisticsCache.get(cluster.get()).getMetrics()
        )
    ).getBody();

    return Mono.just(
        ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("text/plain; version=0.0.4"))
            .body(metrics)
    );
  }
}
