package io.kafbat.ui.service;

import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import io.kafbat.ui.model.ClusterFeature;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

  private final AdminClientService adminClientService;
  private final FeatureService featureService;
  private final StatisticsCache cache;

  public Mono<Statistics> updateCache(KafkaCluster c) {
    return getStatistics(c).doOnSuccess(m -> cache.replace(c, m));
  }

  @SuppressWarnings("unchecked")
  private Mono<Statistics> getStatistics(KafkaCluster cluster) {
    return adminClientService.get(cluster).flatMap(ac ->
        ac.describeCluster()
            .flatMap(description ->
                ac.updateInternalStats(description.getController())
                    .then(
                        Mono.zip(
                            featureService.getAvailableFeatures(ac, cluster, description),
                            loadClusterState(description, ac)
                        ).flatMap(t ->
                            scrapeMetrics(cluster, t.getT2(), description)
                                .map(metrics -> createStats(description, t.getT1(), t.getT2(), metrics, ac)))))
            .doOnError(e ->
                log.error("Failed to collect cluster {} info", cluster.getName(), e))
            .onErrorResume(t -> Mono.just(Statistics.statsUpdateError(t))));
  }

  private Statistics createStats(ClusterDescription description,
                                 List<ClusterFeature> features,
                                 ScrapedClusterState scrapedClusterState,
                                 Metrics metrics,
                                 ReactiveAdminClient ac) {
    return Statistics.builder()
        .status(ServerStatusDTO.ONLINE)
        .clusterDescription(description)
        .version(ac.getVersion())
        .metrics(metrics)
        .features(features)
        .clusterState(scrapedClusterState)
        .build();
  }

  private Mono<ScrapedClusterState> loadClusterState(ClusterDescription clusterDescription,
                                                     ReactiveAdminClient ac) {
    return ScrapedClusterState.scrape(clusterDescription, ac);
  }

  private Mono<Metrics> scrapeMetrics(KafkaCluster cluster,
                                      ScrapedClusterState clusterState,
                                      ClusterDescription clusterDescription) {
    return cluster.getMetricsScrapping()
        .scrape(clusterState, clusterDescription.getNodes());
  }

}
