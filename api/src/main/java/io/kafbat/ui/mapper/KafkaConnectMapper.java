package io.kafbat.ui.mapper;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.model.ClusterInfo;
import io.kafbat.ui.connect.model.ConnectorStatusConnector;
import io.kafbat.ui.connect.model.ConnectorTask;
import io.kafbat.ui.connect.model.NewConnector;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TaskStatusDTO;
import io.kafbat.ui.model.connect.InternalConnectorInfo;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KafkaConnectMapper {
  NewConnector toClient(io.kafbat.ui.model.NewConnectorDTO newConnector);

  @Mapping(target = "status", ignore = true)
  @Mapping(target = "connect", ignore = true)
  ConnectorDTO fromClient(io.kafbat.ui.connect.model.Connector connector);

  ConnectorStatusDTO fromClient(ConnectorStatusConnector connectorStatus);

  @Mapping(target = "status", ignore = true)
  TaskDTO fromClient(ConnectorTask connectorTask);

  TaskStatusDTO fromClient(io.kafbat.ui.connect.model.TaskStatus taskStatus);

  ConnectorPluginDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPlugin connectorPlugin);

  ConnectorPluginConfigValidationResponseDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPluginConfigValidationResponse
          connectorPluginConfigValidationResponse);

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
        .clusterId(clusterInfo.getKafkaClusterId());
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
        .failedTasksCount(failedTasksCount);
  }
}
