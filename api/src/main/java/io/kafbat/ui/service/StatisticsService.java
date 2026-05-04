package io.kafbat.ui.service;

import static io.kafbat.ui.api.model.ControllerType.KRAFT;
import static io.kafbat.ui.api.model.ControllerType.UNKNOWN;
import static io.kafbat.ui.api.model.ControllerType.ZOOKEEPER;
import static io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;

import io.kafbat.ui.api.model.ControllerType;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.mapper.QuorumInfoMapper;
import io.kafbat.ui.model.ClusterFeature;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

  private final AdminClientService adminClientService;
  private final KafkaConnectService kafkaConnectService;
  private final FeatureService featureService;
  private final StatisticsCache cache;
  private final ClustersProperties clustersProperties;
  private final QuorumInfoMapper quorumInfoMapper;

  public Mono<Statistics> updateCache(KafkaCluster c) {
    return getStatistics(c).doOnSuccess(m -> cache.replace(c, m));
  }

  private record LoadQuorumInfoResult(Optional<QuorumInfo> quorumInfo, ControllerType controllerType) {}

  private Mono<Statistics> getStatistics(KafkaCluster cluster) {
    return adminClientService.get(cluster).flatMap(ac ->
        ac.describeCluster()
            .flatMap(description ->
                ac.updateInternalStats(description.getController())
                    .then(
                        Mono.zip(
                            featureService.getAvailableFeatures(ac, cluster, description),
                            loadClusterState(description, ac),
                            loadKafkaConnects(cluster),
                            loadQuorumInfo(ac)
                                .map(quorumInfo -> new LoadQuorumInfoResult(Optional.of(quorumInfo), KRAFT))
                                .onErrorResume(StatisticsService::handleQuorumInfoErrors)))
                        .flatMap(t ->
                            scrapeMetrics(cluster, t.getT2(), description)
                                .map(metrics -> createStats(description,
                                    t.getT1(),
                                    t.getT2(),
                                    t.getT3(),
                                    metrics,
                                    t.getT4().controllerType,
                                    t.getT4().quorumInfo,
                                    ac))
                        )
                    )
            ).doOnError(e ->
                log.error("Failed to collect cluster {} info", cluster.getName(), e)
            ).doOnError(e -> adminClientService.invalidate(cluster, e))
            .onErrorResume(t -> Mono.just(Statistics.statsUpdateError(t)));
  }

  @NotNull
  private static Mono<LoadQuorumInfoResult> handleQuorumInfoErrors(Throwable quorumInfoFetchError) {
    if (quorumInfoFetchError instanceof UnsupportedVersionException) {
      return Mono.just(new LoadQuorumInfoResult(Optional.empty(), ZOOKEEPER));
    }
    else if (quorumInfoFetchError instanceof ClusterAuthorizationException) {
      final String message = "Failed to fetch quorum info due to ClusterAuthorizationException." +
          " In some managed Kafka offerings (e.g. Confluent Cloud), it is prohibited to fetch the quorum information from a cluster, regardless of your role configuration." +
          " In that case, this error is expected behavior.";
      log.warn(message, quorumInfoFetchError);
      return Mono.just(new LoadQuorumInfoResult(Optional.empty(), UNKNOWN));
    }
    return Mono.error(quorumInfoFetchError);
  }

  @NotNull
  private static Mono<QuorumInfo> loadQuorumInfo(ReactiveAdminClient ac) {
    return ac.describeMetadataQuorum();
  }

  private Statistics createStats(ClusterDescription description,
                                 List<ClusterFeature> features,
                                 ScrapedClusterState scrapedClusterState,
                                 List<KafkaConnectState> connects,
                                 Metrics metrics,
                                 ControllerType controllerType,
                                 Optional<QuorumInfo> quorumInfo,
                                 ReactiveAdminClient ac) {
    var stats = Statistics.builder()
        .status(ServerStatusDTO.ONLINE)
        .clusterDescription(description)
        .version(ac.getVersion())
        .metrics(metrics)
        .features(features)
        .clusterState(scrapedClusterState)
        .connectStates(
            connects.stream().collect(
                Collectors.toMap(KafkaConnectState::getName, c -> c)
            )
        )
        .controller(controllerType);

    quorumInfo.ifPresent(info -> stats.quorumInfo(quorumInfoMapper.toInternalQuorumInfo(info)));

    return stats.build();
  }

  private Mono<ScrapedClusterState> loadClusterState(ClusterDescription clusterDescription,
                                                     ReactiveAdminClient ac) {
    return ScrapedClusterState.scrape(clusterDescription, ac, clustersProperties);
  }

  private Mono<Metrics> scrapeMetrics(KafkaCluster cluster,
                                      ScrapedClusterState clusterState,
                                      ClusterDescription clusterDescription) {
    return cluster.getMetricsScrapping()
        .scrape(clusterState, clusterDescription.getNodes());
  }

  private Mono<List<KafkaConnectState>> loadKafkaConnects(KafkaCluster cluster) {
    return kafkaConnectService.scrapeAllConnects(cluster).collectList();
  }

}
