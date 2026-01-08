package io.kafbat.ui.controller;

import static io.kafbat.ui.model.ConnectorActionDTO.RESTART;
import static io.kafbat.ui.model.ConnectorActionDTO.RESTART_ALL_TASKS;
import static io.kafbat.ui.model.ConnectorActionDTO.RESTART_FAILED_TASKS;

import io.kafbat.ui.api.KafkaConnectApi;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorActionDTO;
import io.kafbat.ui.model.ConnectorColumnsToSortDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.NewConnectorDTO;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConnectorAction;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.mcp.McpTool;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
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
public class KafkaConnectController extends AbstractController implements KafkaConnectApi, McpTool {
  private static final Set<ConnectorActionDTO> RESTART_ACTIONS
      = Set.of(RESTART, RESTART_FAILED_TASKS, RESTART_ALL_TASKS);
  private static final String CONNECTOR_NAME = "connectorName";

  private final KafkaConnectService kafkaConnectService;

  @Override
  public Mono<ResponseEntity<Flux<ConnectDTO>>> getConnects(String clusterName,
                                                            Boolean withStats,
                                                            ServerWebExchange exchange) {

    Flux<ConnectDTO> availableConnects = kafkaConnectService.getConnects(
        getCluster(clusterName), withStats != null ? withStats : false
        ).filterWhen(dto -> accessControlService.isConnectAccessible(dto, clusterName));

    return Mono.just(ResponseEntity.ok(availableConnects));
  }

  @Override
  public Mono<ResponseEntity<String>> getConnectsCsv(String clusterName, Boolean withStats,
                                                     ServerWebExchange exchange) {
    return getConnects(clusterName, withStats, exchange)
        .flatMap(this::responseToCsv);
  }

  @Override
  public Mono<ResponseEntity<Flux<String>>> getConnectors(String clusterName, String connectName,
                                                          ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectActions(connectName, ConnectAction.VIEW)
        .operationName("getConnectors")
        .build();

    return validateAccess(context)
        .thenReturn(
            ResponseEntity.ok(
              kafkaConnectService.getConnectors(getCluster(clusterName), connectName)
                  .flatMapMany(m -> Flux.fromIterable(m.keySet()))
            )
        ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ConnectorDTO>> createConnector(String clusterName, String connectName,
                                                            @Valid Mono<NewConnectorDTO> connector,
                                                            ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectActions(connectName, ConnectAction.CREATE)
        .operationName("createConnector")
        .build();

    return validateAccess(context).then(
        kafkaConnectService.createConnector(getCluster(clusterName), connectName, connector)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ConnectorDTO>> getConnector(String clusterName, String connectName,
                                                         String connectorName,
                                                         ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW)
        .operationName("getConnector")
        .build();

    return validateAccess(context).then(
        kafkaConnectService.getConnector(getCluster(clusterName), connectName, connectorName)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteConnector(String clusterName, String connectName,
                                                    String connectorName,
                                                    ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.DELETE)
        .operationName("deleteConnector")
        .build();

    return validateAccess(context).then(
        kafkaConnectService.deleteConnector(getCluster(clusterName), connectName, connectorName)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }


  @Override
  public Mono<ResponseEntity<Flux<FullConnectorInfoDTO>>> getAllConnectors(
      String clusterName,
      String search,
      ConnectorColumnsToSortDTO orderBy,
      SortOrderDTO sortOrder,
      Boolean fts,
      ServerWebExchange exchange
  ) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .operationName("getAllConnectors")
        .build();

    var maybeComparator = Optional.ofNullable(orderBy).map(this::getConnectorsComparator);

    var comparator = sortOrder == null || sortOrder.equals(SortOrderDTO.ASC)
        ? maybeComparator
        : maybeComparator.map(Comparator::reversed);

    Flux<FullConnectorInfoDTO> connectors = kafkaConnectService.getAllConnectors(getCluster(clusterName), search, fts)
        .filterWhen(dto -> accessControlService.isConnectAccessible(dto.getConnect(), clusterName));

    Flux<FullConnectorInfoDTO> sorted = comparator.map(connectors::sort).orElse(connectors);

    return Mono.just(ResponseEntity.ok(sorted))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<String>> getAllConnectorsCsv(String clusterName, String search,
                                                          ConnectorColumnsToSortDTO orderBy,
                                                          SortOrderDTO sortOrder, Boolean fts,
                                                          ServerWebExchange exchange) {
    return getAllConnectors(clusterName, search, orderBy, sortOrder, fts, exchange)
        .flatMap(this::responseToCsv);
  }

  @Override
  public Mono<ResponseEntity<Map<String, Object>>> getConnectorConfig(String clusterName,
                                                                      String connectName,
                                                                      String connectorName,
                                                                      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW)
        .operationName("getConnectorConfig")
        .build();

    return validateAccess(context).then(
        kafkaConnectService
            .getConnectorConfig(getCluster(clusterName), connectName, connectorName)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ConnectorDTO>> setConnectorConfig(String clusterName, String connectName,
                                                               String connectorName,
                                                               Mono<Map<String, Object>> requestBody,
                                                               ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW, ConnectorAction.EDIT)
        .operationName("setConnectorConfig")
        .build();

    return validateAccess(context).then(
            kafkaConnectService
                .setConnectorConfig(getCluster(clusterName), connectName, connectorName, requestBody)
                .map(ResponseEntity::ok))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> updateConnectorState(String clusterName, String connectName,
                                                         String connectorName,
                                                         ConnectorActionDTO action,
                                                         ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW, ConnectorAction.OPERATE)
        .operationName("updateConnectorState")
        .build();

    return validateAccess(context).then(
        kafkaConnectService
            .updateConnectorState(getCluster(clusterName), connectName, connectorName, action)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Flux<TaskDTO>>> getConnectorTasks(String clusterName,
                                                               String connectName,
                                                               String connectorName,
                                                               ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW)
        .operationName("getConnectorTasks")
        .build();

    return validateAccess(context).thenReturn(
        ResponseEntity
            .ok(kafkaConnectService
                .getConnectorTasks(getCluster(clusterName), connectName, connectorName))
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> restartConnectorTask(String clusterName, String connectName,
                                                         String connectorName, Integer taskId,
                                                         ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW, ConnectorAction.OPERATE)
        .operationName("restartConnectorTask")
        .build();

    return validateAccess(context).then(
        kafkaConnectService
            .restartConnectorTask(getCluster(clusterName), connectName, connectorName, taskId)
            .map(ResponseEntity::ok)
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Flux<ConnectorPluginDTO>>> getConnectorPlugins(
      String clusterName, String connectName, ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectActions(connectName, ConnectAction.VIEW)
        .operationName("getConnectorPlugins")
        .build();

    return validateAccess(context).then(
        Mono.just(
            ResponseEntity.ok(
                kafkaConnectService.getConnectorPlugins(getCluster(clusterName), connectName)))
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ConnectorPluginConfigValidationResponseDTO>> validateConnectorPluginConfig(
      String clusterName, String connectName, String pluginName, @Valid Mono<Map<String, Object>> requestBody,
      ServerWebExchange exchange) {
    return kafkaConnectService
        .validateConnectorPluginConfig(
            getCluster(clusterName), connectName, pluginName, requestBody)
        .map(ResponseEntity::ok);
  }

  private Comparator<FullConnectorInfoDTO> getConnectorsComparator(ConnectorColumnsToSortDTO orderBy) {
    var defaultComparator = Comparator.comparing(
        FullConnectorInfoDTO::getName,
        Comparator.nullsFirst(Comparator.naturalOrder())
    );

    return switch (orderBy) {
      case CONNECT -> Comparator.comparing(
          FullConnectorInfoDTO::getConnect,
          Comparator.nullsFirst(Comparator.naturalOrder())
      );
      case TYPE -> Comparator.comparing(
          FullConnectorInfoDTO::getType,
          Comparator.nullsFirst(Comparator.naturalOrder())
      );
      case STATUS -> Comparator.comparing(
          fullConnectorInfoDTO -> fullConnectorInfoDTO.getStatus().getState(),
          Comparator.nullsFirst(Comparator.naturalOrder())
      );
      default -> defaultComparator;
    };
  }

  @Override
  public Mono<ResponseEntity<Void>> resetConnectorOffsets(
      String clusterName,
      String connectName,
      String connectorName,
      ServerWebExchange exchange) {

    var context = AccessContext.builder()
        .cluster(clusterName)
        .connectorActions(connectName, connectorName, ConnectorAction.VIEW, ConnectorAction.RESET_OFFSETS)
        .operationName("resetConnectorOffsets")
        .build();

    return validateAccess(context).then(
        kafkaConnectService
            .resetConnectorOffsets(getCluster(clusterName), connectName, connectorName)
            .map(ResponseEntity::ok))
        .doOnEach(sig -> audit(context, sig));
  }
}
