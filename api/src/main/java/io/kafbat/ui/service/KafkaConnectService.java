package io.kafbat.ui.service;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.api.KafkaConnectClientApi;
import io.kafbat.ui.connect.model.ClusterInfo;
import io.kafbat.ui.connect.model.ConnectorExpand;
import io.kafbat.ui.connect.model.ConnectorStatus;
import io.kafbat.ui.connect.model.ConnectorStatusConnector;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.connect.model.TaskStatus;
import io.kafbat.ui.exception.ConnectorOffsetsResetException;
import io.kafbat.ui.exception.NotFoundException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.mapper.KafkaConnectMapper;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorActionDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.NewConnectorDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TaskIdDTO;
import io.kafbat.ui.model.connect.InternalConnectorInfo;
import io.kafbat.ui.service.index.KafkaConnectNgramFilter;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import io.kafbat.ui.util.ReactiveFailover;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Service
@Slf4j
public class KafkaConnectService {
  private final KafkaConnectMapper kafkaConnectMapper;
  private final KafkaConfigSanitizer kafkaConfigSanitizer;
  private final ClustersProperties clustersProperties;
  private final StatisticsCache statisticsCache;

  public KafkaConnectService(KafkaConnectMapper kafkaConnectMapper,
                             KafkaConfigSanitizer kafkaConfigSanitizer,
                             ClustersProperties clustersProperties,
                             StatisticsCache statisticsCache) {
    this.kafkaConnectMapper = kafkaConnectMapper;
    this.kafkaConfigSanitizer = kafkaConfigSanitizer;
    this.clustersProperties = clustersProperties;
    this.statisticsCache = statisticsCache;
  }

  public Flux<ConnectDTO> getConnects(KafkaCluster cluster, boolean withStats) {
    Optional<List<ClustersProperties.@Valid ConnectCluster>> connectClusters =
        Optional.ofNullable(cluster.getOriginalProperties().getKafkaConnect());

    if (withStats) {
      return connectClusters.map(connects ->
              Flux.fromIterable(connects).flatMap(c ->
                  getClusterInfo(cluster, c.getName()).map(ci -> Tuples.of(c, ci))
              ).flatMap(tuple -> (
                  getConnectConnectors(cluster, tuple.getT1())
                      .collectList()
                      .map(connectors ->
                          kafkaConnectMapper.toKafkaConnect(tuple.getT1(), connectors, tuple.getT2(), true)
                      )
              )
          )
      ).orElse(Flux.fromIterable(List.of()));
    } else {
      return Flux.fromIterable(connectClusters.orElse(List.of()))
          .flatMap(c ->
              getClusterInfo(cluster, c.getName()).map(info ->
                  kafkaConnectMapper.toKafkaConnect(c, List.of(), info, false)
              )
          );
    }
  }

  public Mono<ClusterInfo> getClusterInfo(KafkaCluster cluster, String connectName) {
    KafkaConnectState state = statisticsCache.get(cluster).getConnectStates().get(connectName);
    if (state != null) {
      return Mono.just(kafkaConnectMapper.toClient(state));
    } else {
      return api(cluster, connectName).mono(KafkaConnectClientApi::getClusterInfo)
          .onErrorResume(th -> {
            log.error("Error on collecting cluster info", th);
            return Mono.just(new ClusterInfo());
          });
    }
  }

  private Flux<InternalConnectorInfo> getConnectConnectors(
      KafkaCluster cluster,
      ClustersProperties.ConnectCluster connect) {
    return getConnectorsWithErrorsSuppress(cluster, connect.getName()).flatMapMany(connectors ->
        Flux.fromStream(
            connectors.values().stream().map(c ->
                kafkaConnectMapper.fromClient(connect.getName(), c, null)
            )
        )
    );
  }

  public Flux<FullConnectorInfoDTO> getAllConnectors(final KafkaCluster cluster,
                                                     @Nullable final String search, Boolean fts) {
    return getConnects(cluster, false)
        .flatMap(connect ->
            getConnectorsWithErrorsSuppress(cluster, connect.getName())
                .flatMapMany(connectors ->
                    Flux.fromIterable(connectors.entrySet())
                        .flatMap(e ->
                          getConnectorTopics(
                              cluster,
                              connect.getName(),
                              e.getKey()
                          ).map(topics ->
                              kafkaConnectMapper.fromClient(connect.getName(), e.getValue(), topics.getTopics())
                          )
                        )
                )
        ).map(kafkaConnectMapper::fullConnectorInfo)
        .collectList()
        .map(lst -> filterConnectors(lst, search, fts))
        .flatMapMany(Flux::fromIterable);
  }

  public Flux<KafkaConnectState> scrapeAllConnects(KafkaCluster cluster) {

    Optional<List<ClustersProperties.@Valid ConnectCluster>> connectClusters =
        Optional.ofNullable(cluster.getOriginalProperties().getKafkaConnect());

    return Flux.fromIterable(connectClusters.orElse(List.of())).flatMap(c ->
        getClusterInfo(cluster, c.getName()).map(info ->
                kafkaConnectMapper.toKafkaConnect(c, List.of(), info, false)
        ).onErrorResume((t) -> Mono.just(new ConnectDTO().name(c.getName())))
    ).flatMap(connect ->
        getConnectorsWithErrorsSuppress(cluster, connect.getName())
            .onErrorResume(t -> Mono.just(Map.of()))
            .flatMapMany(connectors ->
                Flux.fromIterable(connectors.entrySet())
                    .flatMap(e ->
                        getConnectorTopics(
                            cluster,
                            connect.getName(),
                            e.getKey()
                        ).map(topics ->
                            kafkaConnectMapper.fromClient(connect.getName(), e.getValue(), topics.getTopics())
                        )
                    )
            ).collectList().map(connectors -> kafkaConnectMapper.toScrapeState(connect, connectors))
    );
  }

  private List<FullConnectorInfoDTO> filterConnectors(
      List<FullConnectorInfoDTO> connectors,
      String search,
      Boolean fts) {
    boolean useFts = clustersProperties.getFts().use(fts);
    KafkaConnectNgramFilter filter =
        new KafkaConnectNgramFilter(connectors, useFts, clustersProperties.getFts().getConnect());
    return filter.find(search);
  }

  public Mono<ConnectorTopics> getConnectorTopics(KafkaCluster cluster, String connectClusterName,
                                                  String connectorName) {
    return api(cluster, connectClusterName)
        .mono(c -> c.getConnectorTopics(connectorName))
        .map(result -> result.get(connectorName))
        // old Connect API versions don't have this endpoint, setting empty list for
        // backward-compatibility
        .onErrorResume(Exception.class, e -> Mono.just(new ConnectorTopics().topics(List.of())));
  }

  public Mono<Map<String, ExpandedConnector>> getConnectors(KafkaCluster cluster, String connectName) {
    return api(cluster, connectName)
        .mono(client ->
            client.getConnectors(null, List.of(ConnectorExpand.INFO, ConnectorExpand.STATUS))
        );
  }

  // returns empty flux if there was an error communicating with Connect
  public Mono<Map<String, ExpandedConnector>> getConnectorsWithErrorsSuppress(
      KafkaCluster cluster, String connectName) {
    return getConnectors(cluster, connectName).onErrorComplete();
  }

  public Mono<ConnectorDTO> createConnector(KafkaCluster cluster, String connectName,
                                            Mono<NewConnectorDTO> connector) {
    return api(cluster, connectName)
        .mono(client ->
            connector
                .flatMap(c -> connectorExists(cluster, connectName, c.getName())
                    .flatMap(exists -> {
                      if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new ValidationException(
                            String.format("Connector with name %s already exists", c.getName())));
                      } else {
                        return Mono.just(c);
                      }
                    }))
                .map(kafkaConnectMapper::toClient)
                .flatMap(client::createConnector)
                .flatMap(c -> getConnector(cluster, connectName, c.getName()))
        );
  }

  private Mono<Boolean> connectorExists(KafkaCluster cluster, String connectName,
                                        String connectorName) {
    return getConnectors(cluster, connectName)
        .map(m -> m.containsKey(connectorName));
  }

  public Mono<ConnectorDTO> getConnector(KafkaCluster cluster, String connectName,
                                         String connectorName) {
    return api(cluster, connectName)
        .mono(client ->
            Mono.zip(
                client.getConnector(connectorName),
                getConnectorTopics(cluster, connectName, connectorName),
                client.getConnectorStatus(connectorName).onErrorResume(WebClientResponseException.NotFound.class,
                        e -> emptyStatus(connectorName))
            )
            .map(t ->
                kafkaConnectMapper.fromClient(
                    t.getT1(),
                    connectName,
                    t.getT2(),
                    kafkaConfigSanitizer.sanitizeConnectorConfig(t.getT1().getConfig()),
                    t.getT3()
                )
            )
        );
  }

  private Mono<ConnectorStatus> emptyStatus(String connectorName) {
    return Mono.just(new ConnectorStatus()
        .name(connectorName)
        .tasks(List.of())
        .connector(new ConnectorStatusConnector()
            .state(ConnectorStatusConnector.StateEnum.UNASSIGNED)));
  }

  public Mono<Map<String, Object>> getConnectorConfig(KafkaCluster cluster, String connectName,
                                                      String connectorName) {
    return api(cluster, connectName)
        .mono(c -> c.getConnectorConfig(connectorName))
        .map(kafkaConfigSanitizer::sanitizeConnectorConfig);
  }

  public Mono<ConnectorDTO> setConnectorConfig(KafkaCluster cluster, String connectName,
                                               String connectorName, Mono<Map<String, Object>> requestBody) {
    return api(cluster, connectName)
        .mono(c ->
            requestBody
                .flatMap(body -> c.setConnectorConfig(connectorName, body))
                .map(connector -> kafkaConnectMapper.fromClient(connector)));
  }

  public Mono<Void> deleteConnector(
      KafkaCluster cluster, String connectName, String connectorName) {
    return api(cluster, connectName)
        .mono(c -> c.deleteConnector(connectorName));
  }

  public Mono<Void> updateConnectorState(KafkaCluster cluster, String connectName,
                                         String connectorName, ConnectorActionDTO action) {
    return api(cluster, connectName)
        .mono(client -> switch (action) {
              case RESTART -> client.restartConnector(connectorName, false, false);
              case RESTART_ALL_TASKS -> restartTasks(cluster, connectName, connectorName, task -> true);
              case RESTART_FAILED_TASKS -> restartTasks(cluster, connectName, connectorName,
                  t -> t.getStatus().getState() == ConnectorTaskStatusDTO.FAILED);
              case PAUSE -> client.pauseConnector(connectorName);
              case STOP -> client.stopConnector(connectorName);
              case RESUME -> client.resumeConnector(connectorName);
            }
        );
  }

  private Mono<Void> restartTasks(KafkaCluster cluster, String connectName,
                                  String connectorName, Predicate<TaskDTO> taskFilter) {
    return getConnectorTasks(cluster, connectName, connectorName)
        .filter(taskFilter)
        .flatMap(t ->
            restartConnectorTask(
                cluster, connectName, connectorName,
                Optional.ofNullable(t.getId()).map(TaskIdDTO::getTask).orElseThrow()
            )
        ).then();
  }

  public Flux<TaskDTO> getConnectorTasks(KafkaCluster cluster, String connectName, String connectorName) {
    return api(cluster, connectName)
        .flux(client ->
            client.getConnectorTasks(connectorName)
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Flux.empty())
                .map(kafkaConnectMapper::fromClient)
                .flatMap(task ->
                    client
                        .getConnectorTaskStatus(connectorName,
                            Optional.ofNullable(task.getId()).map(TaskIdDTO::getTask).orElseThrow()
                        ).onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                        .map(kafkaConnectMapper::fromClient)
                        .map(task::status)
                ));
  }

  public Mono<Void> restartConnectorTask(KafkaCluster cluster, String connectName,
                                         String connectorName, Integer taskId) {
    return api(cluster, connectName)
        .mono(client -> client.restartConnectorTask(connectorName, taskId));
  }

  public Flux<ConnectorPluginDTO> getConnectorPlugins(KafkaCluster cluster,
                                                      String connectName) {
    return api(cluster, connectName)
        .flux(client -> client.getConnectorPlugins().map(kafkaConnectMapper::fromClient));
  }

  public Mono<ConnectorPluginConfigValidationResponseDTO> validateConnectorPluginConfig(
      KafkaCluster cluster, String connectName, String pluginName, Mono<Map<String, Object>> requestBody) {
    return api(cluster, connectName)
        .mono(client ->
            requestBody
                .flatMap(body ->
                    client.validateConnectorPluginConfig(pluginName, body))
                .map(kafkaConnectMapper::fromClient)
        );
  }

  private ReactiveFailover<KafkaConnectClientApi> api(KafkaCluster cluster, String connectName) {
    var client = cluster.getConnectsClients().get(connectName);
    if (client == null) {
      throw new NotFoundException(
          "Connect %s not found for cluster %s".formatted(connectName, cluster.getName()));
    }
    return client;
  }

  public Mono<Void> resetConnectorOffsets(KafkaCluster cluster, String connectName,
      String connectorName) {
    return api(cluster, connectName)
        .mono(client -> client.resetConnectorOffsets(connectorName))
        .onErrorResume(WebClientResponseException.NotFound.class,
            e -> {
              throw new NotFoundException("Connector %s not found in %s".formatted(connectorName, connectName));
            })
        .onErrorResume(WebClientResponseException.BadRequest.class,
            e -> {
              throw new ConnectorOffsetsResetException(
                  "Failed to reset offsets of connector %s of %s. Make sure it is STOPPED first."
                      .formatted(connectorName, connectName));
            });
  }

  public Flux<FullConnectorInfoDTO> getTopicConnectors(KafkaCluster cluster, String topicName) {
    Map<String, KafkaConnectState> connectStates = this.statisticsCache.get(cluster).getConnectStates();
    Map<String, List<String>> filteredConnects = new HashMap<>();
    for (Map.Entry<String, KafkaConnectState> entry : connectStates.entrySet()) {
      List<KafkaConnectState.ConnectorState> connectors =
          entry.getValue().getConnectors().stream().filter(c -> c.topics().contains(topicName)).toList();
      if (!connectors.isEmpty()) {
        filteredConnects.put(entry.getKey(), connectors.stream().map(KafkaConnectState.ConnectorState::name).toList());
      }
    }

    return Flux.fromIterable(filteredConnects.entrySet())
        .flatMap(entry ->
            getConnectorsWithErrorsSuppress(cluster, entry.getKey())
                .map(connectors ->
                        connectors.entrySet()
                            .stream()
                            .filter(c -> entry.getValue().contains(c.getKey()))
                            .map(c -> kafkaConnectMapper.fromClient(entry.getKey(), c.getValue(), null))
                            .map(kafkaConnectMapper::fullConnectorInfo)
                            .toList()
                )
        ).flatMap(Flux::fromIterable);

  }
}
