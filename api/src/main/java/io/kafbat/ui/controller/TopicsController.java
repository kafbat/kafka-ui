package io.kafbat.ui.controller;

import static io.kafbat.ui.model.rbac.permission.TopicAction.ANALYSIS_RUN;
import static io.kafbat.ui.model.rbac.permission.TopicAction.ANALYSIS_VIEW;
import static io.kafbat.ui.model.rbac.permission.TopicAction.CREATE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.DELETE;
import static io.kafbat.ui.model.rbac.permission.TopicAction.EDIT;
import static io.kafbat.ui.model.rbac.permission.TopicAction.VIEW;
import static java.util.stream.Collectors.toList;

import io.kafbat.ui.api.TopicsApi;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import io.kafbat.ui.model.PartitionsIncreaseDTO;
import io.kafbat.ui.model.PartitionsIncreaseResponseDTO;
import io.kafbat.ui.model.ReplicationFactorChangeDTO;
import io.kafbat.ui.model.ReplicationFactorChangeResponseDTO;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.TopicAnalysisDTO;
import io.kafbat.ui.model.TopicColumnsToSortDTO;
import io.kafbat.ui.model.TopicConfigDTO;
import io.kafbat.ui.model.TopicCreationDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.model.TopicDetailsDTO;
import io.kafbat.ui.model.TopicProducerStateDTO;
import io.kafbat.ui.model.TopicUpdateDTO;
import io.kafbat.ui.model.TopicsResponseDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.TopicsService;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.kafbat.ui.service.mcp.McpTool;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TopicsController extends AbstractController implements TopicsApi, McpTool {

  private static final Integer DEFAULT_PAGE_SIZE = 25;

  private final TopicsService topicsService;
  private final TopicAnalysisService topicAnalysisService;
  private final ClusterMapper clusterMapper;
  private final ClustersProperties clustersProperties;
  private final KafkaConnectService kafkaConnectService;

  @Override
  public Mono<ResponseEntity<TopicDTO>> createTopic(
      String clusterName, @Valid Mono<TopicCreationDTO> topicCreationMono, ServerWebExchange exchange) {
    return topicCreationMono.flatMap(topicCreation -> {
      var context = AccessContext.builder()
          .cluster(clusterName)
          .topicActions(topicCreation.getName(), CREATE)
          .operationName("createTopic")
          .operationParams(topicCreation)
          .build();

      return validateAccess(context)
          .then(topicsService.createTopic(getCluster(clusterName), topicCreation))
          .map(clusterMapper::toTopic)
          .map(s -> new ResponseEntity<>(s, HttpStatus.OK))
          .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
          .doOnEach(sig -> audit(context, sig));
    });
  }

  @Override
  public Mono<ResponseEntity<TopicDTO>> recreateTopic(String clusterName,
                                                      String topicName, ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW, CREATE, DELETE)
        .operationName("recreateTopic")
        .build();

    return validateAccess(context).then(
        topicsService.recreateTopic(getCluster(clusterName), topicName)
            .map(clusterMapper::toTopic)
            .map(s -> new ResponseEntity<>(s, HttpStatus.CREATED))
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<TopicDTO>> cloneTopic(
      String clusterName, String topicName, String newTopicName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW, CREATE)
        .operationName("cloneTopic")
        .operationParams(Map.of("newTopicName", newTopicName))
        .build();

    return validateAccess(context)
        .then(topicsService.cloneTopic(getCluster(clusterName), topicName, newTopicName)
            .map(clusterMapper::toTopic)
            .map(s -> new ResponseEntity<>(s, HttpStatus.CREATED))
        ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteTopic(
      String clusterName, String topicName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, DELETE)
        .operationName("deleteTopic")
        .build();

    return validateAccess(context)
        .then(
            topicsService.deleteTopic(getCluster(clusterName), topicName)
                .thenReturn(ResponseEntity.ok().<Void>build())
        ).doOnEach(sig -> audit(context, sig));
  }


  @Override
  public Mono<ResponseEntity<Flux<TopicConfigDTO>>> getTopicConfigs(
      String clusterName, String topicName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW)
        .operationName("getTopicConfigs")
        .build();

    return validateAccess(context).then(
        topicsService.getTopicConfigs(getCluster(clusterName), topicName)
            .map(lst -> lst.stream()
                .map(InternalTopicConfig::from)
                .map(clusterMapper::toTopicConfig)
                .toList())
            .map(Flux::fromIterable)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<TopicDetailsDTO>> getTopicDetails(
      String clusterName, String topicName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW)
        .operationName("getTopicDetails")
        .build();

    return validateAccess(context).then(
        topicsService.getTopicDetails(getCluster(clusterName), topicName)
            .map(clusterMapper::toTopicDetails)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<TopicsResponseDTO>> getTopics(String clusterName,
                                                           @Valid Integer page,
                                                           @Valid Integer perPage,
                                                           @Valid Boolean showInternal,
                                                           @Valid String search,
                                                           @Valid TopicColumnsToSortDTO orderBy,
                                                           @Valid SortOrderDTO sortOrder,
                                                           Boolean fts,
                                                           ServerWebExchange exchange) {

    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getTopics")
        .build();

    return topicsService.getTopicsForPagination(getCluster(clusterName), search, showInternal, fts)
        .flatMap(topics -> accessControlService.filterViewableTopics(topics, clusterName))
        .flatMap(topics -> {
          int pageSize = perPage != null && perPage > 0 ? perPage : DEFAULT_PAGE_SIZE;
          var topicsToSkip = ((page != null && page > 0 ? page : 1) - 1) * pageSize;
          ClustersProperties.ClusterFtsProperties ftsProperties = clustersProperties.getFts();
          boolean useFts = ftsProperties.use(fts);
          Comparator<InternalTopic> comparatorForTopic = getComparatorForTopic(orderBy, useFts);
          var comparator = sortOrder == null || !sortOrder.equals(SortOrderDTO.DESC)
              ? comparatorForTopic : comparatorForTopic.reversed();

          List<InternalTopic> filtered = topics.stream().sorted(comparator).toList();

          var totalPages = (filtered.size() / pageSize)
              + (filtered.size() % pageSize == 0 ? 0 : 1);

          List<String> topicsPage = filtered.stream()
              .filter(t -> !t.isInternal() || showInternal != null && showInternal)
              .skip(topicsToSkip)
              .limit(pageSize)
              .map(InternalTopic::getName)
              .collect(toList());

          return topicsService.loadTopics(getCluster(clusterName), topicsPage)
              .map(topicsToRender ->
                  new TopicsResponseDTO()
                      .topics(topicsToRender.stream().map(clusterMapper::toTopic).toList())
                      .pageCount(totalPages));
        })
        .map(ResponseEntity::ok)
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<TopicDTO>> updateTopic(
      String clusterName, String topicName, @Valid Mono<TopicUpdateDTO> topicUpdate,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW, EDIT)
        .operationName("updateTopic")
        .build();

    return validateAccess(context).then(
        topicsService
            .updateTopic(getCluster(clusterName), topicName, topicUpdate)
            .map(clusterMapper::toTopic)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<PartitionsIncreaseResponseDTO>> increaseTopicPartitions(
      String clusterName, String topicName,
      Mono<PartitionsIncreaseDTO> partitionsIncrease,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW, EDIT)
        .build();

    return validateAccess(context).then(
        partitionsIncrease.flatMap(partitions ->
            topicsService.increaseTopicPartitions(getCluster(clusterName), topicName, partitions)
        ).map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ReplicationFactorChangeResponseDTO>> changeReplicationFactor(
      String clusterName, String topicName,
      Mono<ReplicationFactorChangeDTO> replicationFactorChange,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW, EDIT)
        .operationName("changeReplicationFactor")
        .build();

    return validateAccess(context).then(
        replicationFactorChange
            .flatMap(rfc ->
                topicsService.changeReplicationFactor(getCluster(clusterName), topicName, rfc))
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> analyzeTopic(String clusterName, String topicName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, ANALYSIS_RUN)
        .operationName("analyzeTopic")
        .build();

    return validateAccess(context).then(
        topicAnalysisService.analyze(getCluster(clusterName), topicName)
            .doOnEach(sig -> audit(context, sig))
            .thenReturn(ResponseEntity.ok().build())
    );
  }

  @Override
  public Mono<ResponseEntity<Void>> cancelTopicAnalysis(String clusterName, String topicName,
                                                        ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, ANALYSIS_RUN)
        .operationName("cancelTopicAnalysis")
        .build();

    return validateAccess(context)
        .then(Mono.fromRunnable(() -> topicAnalysisService.cancelAnalysis(getCluster(clusterName), topicName)))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }


  @Override
  public Mono<ResponseEntity<TopicAnalysisDTO>> getTopicAnalysis(String clusterName,
                                                                 String topicName,
                                                                 ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, ANALYSIS_VIEW)
        .operationName("getTopicAnalysis")
        .build();

    return validateAccess(context)
        .thenReturn(topicAnalysisService.getTopicAnalysis(getCluster(clusterName), topicName)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build()))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Flux<TopicProducerStateDTO>>> getActiveProducerStates(String clusterName,
                                                                                   String topicName,
                                                                                   ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW)
        .operationName("getActiveProducerStates")
        .build();

    Comparator<TopicProducerStateDTO> ordering =
        Comparator.comparingInt(TopicProducerStateDTO::getPartition)
            .thenComparing(Comparator.comparing(TopicProducerStateDTO::getProducerId).reversed());

    Flux<TopicProducerStateDTO> states = topicsService.getActiveProducersState(getCluster(clusterName), topicName)
        .flatMapMany(statesMap ->
            Flux.fromStream(
                statesMap.entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(p -> clusterMapper.map(e.getKey().partition(), p)))
                    .sorted(ordering)));

    return validateAccess(context)
        .thenReturn(states)
        .map(ResponseEntity::ok)
        .doOnEach(sig -> audit(context, sig));
  }

  private Comparator<InternalTopic> getComparatorForTopic(
      TopicColumnsToSortDTO orderBy,
      boolean ftsEnabled) {
    var defaultComparator = Comparator.comparing(InternalTopic::getName);
    if (orderBy == null && ftsEnabled) {
      return  (o1, o2) -> 0;
    } else if (orderBy == null) {
      return defaultComparator;
    }
    return switch (orderBy) {
      case TOTAL_PARTITIONS -> Comparator.comparing(InternalTopic::getPartitionCount);
      case OUT_OF_SYNC_REPLICAS -> Comparator.comparing(t -> t.getReplicas() - t.getInSyncReplicas());
      case REPLICATION_FACTOR -> Comparator.comparing(InternalTopic::getReplicationFactor);
      case SIZE -> Comparator.comparing(InternalTopic::getSegmentSize);
      case MESSAGES_COUNT ->  Comparator.comparing(
          InternalTopic::getMessagesCount,
          Comparator.nullsFirst(Long::compareTo)
      );
      default -> defaultComparator;
    };
  }

  @Override
  public Mono<ResponseEntity<Flux<FullConnectorInfoDTO>>> getTopicConnectors(String clusterName,
                                                                             String topicName,
                                                                             ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .topicActions(topicName, VIEW)
        .operationName("getTopicConnectors")
        .operationParams(topicName)
        .build();

    Flux<FullConnectorInfoDTO> job = kafkaConnectService.getTopicConnectors(getCluster(clusterName), topicName)
        .filterWhen(dto -> accessControlService.isConnectAccessible(dto.getConnect(), clusterName));

    return validateAccess(context)
        .then(Mono.just(ResponseEntity.ok(job)))
        .doOnEach(sig -> audit(context, sig));
  }
}
