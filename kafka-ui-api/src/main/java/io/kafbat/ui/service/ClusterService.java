package io.kafbat.ui.service;

import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ClusterMetricsDTO;
import io.kafbat.ui.model.ClusterStatsDTO;
import io.kafbat.ui.model.InternalClusterState;
import io.kafbat.ui.model.KafkaCluster;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterService {

  private final StatisticsCache statisticsCache;
  private final ClustersStorage clustersStorage;
  private final ClusterMapper clusterMapper;
  private final StatisticsService statisticsService;

  public List<ClusterDTO> getClusters() {
    return clustersStorage.getKafkaClusters()
        .stream()
        .map(c -> clusterMapper.toCluster(new InternalClusterState(c, statisticsCache.get(c))))
        .collect(Collectors.toList());
  }

  public Mono<ClusterStatsDTO> getClusterStats(KafkaCluster cluster) {
    return Mono.justOrEmpty(
        clusterMapper.toClusterStats(
            new InternalClusterState(cluster, statisticsCache.get(cluster)))
    );
  }

  public Mono<ClusterMetricsDTO> getClusterMetrics(KafkaCluster cluster) {

    return Mono.just(
        clusterMapper.toClusterMetrics(
            statisticsCache.get(cluster).getMetrics()));
  }

  public Mono<ClusterDTO> updateCluster(KafkaCluster cluster) {
    return statisticsService.updateCache(cluster)
        .map(metrics -> clusterMapper.toCluster(new InternalClusterState(cluster, metrics)));
  }
}
