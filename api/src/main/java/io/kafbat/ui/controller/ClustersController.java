package io.kafbat.ui.controller;

import io.kafbat.ui.api.ClustersApi;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ClusterMetricsDTO;
import io.kafbat.ui.model.ClusterStatsDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ClusterService;
import io.kafbat.ui.service.mcp.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ClustersController extends AbstractController implements ClustersApi, McpTool {
  private final ClusterService clusterService;

  @Override
  public Mono<ResponseEntity<Flux<ClusterDTO>>> getClusters(ServerWebExchange exchange) {
    Flux<ClusterDTO> job = Flux.fromIterable(clusterService.getClusters())
        .filterWhen(accessControlService::isClusterAccessible);

    return Mono.just(ResponseEntity.ok(job));
  }

  @Override
  public Mono<ResponseEntity<ClusterMetricsDTO>> getClusterMetrics(String clusterName,
                                                                   ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getClusterMetrics")
        .build();

    return validateAccess(context)
        .then(
            clusterService.getClusterMetrics(getCluster(clusterName))
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build())
        )
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ClusterStatsDTO>> getClusterStats(String clusterName,
                                                               ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getClusterStats")
        .build();

    return validateAccess(context)
        .then(
            clusterService.getClusterStats(getCluster(clusterName))
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build())
        )
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ClusterDTO>> updateClusterInfo(String clusterName,
                                                            ServerWebExchange exchange) {

    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("updateClusterInfo")
        .build();

    return validateAccess(context)
        .then(clusterService.updateCluster(getCluster(clusterName)).map(ResponseEntity::ok))
        .doOnEach(sig -> audit(context, sig));
  }
}
