package io.kafbat.ui.mapper;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.model.ClusterInfo;
import io.kafbat.ui.connect.model.Connector;
import io.kafbat.ui.connect.model.ConnectorStatus;
import io.kafbat.ui.connect.model.ConnectorStatusConnector;
import io.kafbat.ui.connect.model.ConnectorTask;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.connect.model.NewConnector;
import io.kafbat.ui.connect.model.TaskStatus;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TaskIdDTO;
import io.kafbat.ui.model.TaskStatusDTO;
import io.kafbat.ui.model.connect.InternalConnectorInfo;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring")
public interface KafkaConnectMapper {
  NewConnector toClient(io.kafbat.ui.model.NewConnectorDTO newConnector);

  default ClusterInfo toClient(KafkaConnectState state) {
    ClusterInfo clusterInfo = new ClusterInfo();
    clusterInfo.setVersion(state.getVersion());
    return clusterInfo;
  }

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "connect", ignore = true)
  @Mapping(target = "topics", ignore = true)
  ConnectorDTO fromClient(Connector connector);

  default ConnectorDTO fromClient(Connector connector,
                                  ClustersProperties.ConnectCluster properties,
                                  String connect,
                                  ConnectorTopics topics,
                                  Map<String, Object> sanitizedConfigs,
                                  ConnectorStatus status) {
    ConnectorDTO result = this.fromClient(connector);
    result.connect(connect);
    if (topics != null) {
      result = result.topics(topics.getTopics());
    }
    if (sanitizedConfigs != null) {
      result = result.config(sanitizedConfigs);
    }
    if (status != null && status.getConnector() != null) {
      result = result.status(fromClient(status.getConnector()));

      if (status.getTasks() != null) {
        boolean isAnyTaskFailed = status.getTasks().stream()
            .map(TaskStatus::getState)
            .anyMatch(TaskStatus.StateEnum.FAILED::equals);

        if (isAnyTaskFailed) {
          result.getStatus().state(ConnectorStateDTO.TASK_FAILED);
        }
      }
    }
    result.setConsumer(JsonNullable.of(
        properties.getConsumerNamePattern().formatted(connector.getName())
    ));

    return result;
  }

  ConnectorStatusDTO fromClient(ConnectorStatusConnector connectorStatus);

  @Mapping(target = "status", ignore = true)
  TaskDTO fromClient(ConnectorTask connectorTask);

  TaskStatusDTO fromClient(io.kafbat.ui.connect.model.TaskStatus taskStatus);

  ConnectorPluginDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPlugin connectorPlugin);

  ConnectorPluginConfigValidationResponseDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPluginConfigValidationResponse
          connectorPluginConfigValidationResponse);


  default InternalConnectorInfo fromClient(ClustersProperties.ConnectCluster connect,
                                           ExpandedConnector connector, @Nullable List<String> topics) {
    return fromClient(connect.getName(), connect.getConsumerNamePattern(), connector, topics);
  }

  default InternalConnectorInfo fromClient(ConnectDTO connect, ExpandedConnector connector,
                                           @Nullable List<String> topics) {
    return fromClient(connect.getName(), connect.getConsumerNamePattern(), connector, topics);
  }

  default InternalConnectorInfo fromClient(String connectName, String consumerGroupPattern,
                                           ExpandedConnector connector, @Nullable List<String> topics) {
    Objects.requireNonNull(connector.getInfo());
    Objects.requireNonNull(connector.getStatus());
    List<TaskDTO> tasks = List.of();

    if (connector.getInfo().getTasks() != null
        && connector.getStatus().getTasks() != null
    ) {
      Map<Integer, TaskIdDTO> taskIds = connector.getInfo().getTasks()
          .stream().map(t -> new TaskIdDTO().task(t.getTask()).connector(t.getConnector()))
          .collect(Collectors.toMap(
              TaskIdDTO::getTask,
              t -> t
          ));

      tasks = connector.getStatus().getTasks().stream()
          .map(s ->
              new TaskDTO().status(fromClient(s)).id(taskIds.get(s.getId()))
          ).toList();
    }

    ConnectorDTO connectorDto = fromClient(connector.getInfo())
        .connect(connectName)
        .consumer(consumerGroupPattern.formatted(connector.getInfo().getName()))
        .status(fromClient(connector.getStatus().getConnector()));

    return InternalConnectorInfo.builder()
        .connector(connectorDto)
        .config(connector.getInfo().getConfig())
        .tasks(tasks)
        .topics(topics)
        .consumer(consumerGroupPattern.formatted(connector.getInfo().getName()))
        .build();
  }

  default ConnectDTO toKafkaConnect(
      ClustersProperties.ConnectCluster connect,
      List<InternalConnectorInfo> connectors,
      ClusterInfo clusterInfo,
      boolean withStats) {
    Integer connectorCount = null;
    Integer failedConnectors = null;
    Integer tasksCount = null;
    Integer failedTasksCount = null;

    if (withStats) {
      connectorCount = connectors.size();
      failedConnectors = 0;
      tasksCount = 0;
      failedTasksCount = 0;

      for (InternalConnectorInfo connector : connectors) {
        Optional<ConnectorDTO> internalConnector = Optional.ofNullable(connector.getConnector());

        failedConnectors += internalConnector
            .map(ConnectorDTO::getStatus)
            .map(ConnectorStatusDTO::getState)
            .filter(ConnectorStateDTO.FAILED::equals)
            .map(s -> 1).orElse(0);

        tasksCount += internalConnector.map(ConnectorDTO::getTasks).map(List::size).orElse(0);

        if (connector.getTasks() != null) {
          failedTasksCount += (int) connector.getTasks().stream()
              .filter(t ->
                  Optional.ofNullable(t)
                      .map(TaskDTO::getStatus)
                      .map(TaskStatusDTO::getState)
                      .map(ConnectorTaskStatusDTO.FAILED::equals)
                      .orElse(false)
              ).count();
        }
      }

    }

    return new ConnectDTO()
        .address(connect.getAddress())
        .name(connect.getName())
        .connectorsCount(connectorCount)
        .failedConnectorsCount(failedConnectors)
        .tasksCount(tasksCount)
        .failedTasksCount(failedTasksCount)
        .version(clusterInfo.getVersion())
        .commit(clusterInfo.getCommit())
        .clusterId(clusterInfo.getKafkaClusterId())
        .consumerNamePattern(connect.getConsumerNamePattern());
  }

  default FullConnectorInfoDTO fullConnectorInfo(InternalConnectorInfo connectInfo) {
    ConnectorDTO connector = connectInfo.getConnector();
    List<TaskDTO> tasks = connectInfo.getTasks();
    int failedTasksCount = (int) tasks.stream()
        .map(TaskDTO::getStatus)
        .map(TaskStatusDTO::getState)
        .filter(ConnectorTaskStatusDTO.FAILED::equals)
        .count();
    return new FullConnectorInfoDTO()
        .connect(connector.getConnect())
        .name(connector.getName())
        .connectorClass((String) connectInfo.getConfig().get("connector.class"))
        .type(connector.getType())
        .topics(connectInfo.getTopics())
        .status(connector.getStatus())
        .tasksCount(tasks.size())
        .consumer(connectInfo.getConsumer())
        .failedTasksCount(failedTasksCount);
  }

  default KafkaConnectState toScrapeState(ConnectDTO connect, List<InternalConnectorInfo> connectors) {
    return KafkaConnectState.builder()
        .name(connect.getName())
        .version(connect.getVersion().orElse("Unknown"))
        .connectors(connectors.stream().map(this::toScrapeState).toList())
        .build();
  }

  default KafkaConnectState.ConnectorState toScrapeState(InternalConnectorInfo connector) {
    return new KafkaConnectState.ConnectorState(
        connector.getConnector().getName(),
        connector.getConnector().getType(),
        connector.getConnector().getStatus(),
        connector.getTopics()
    );
  }
}
