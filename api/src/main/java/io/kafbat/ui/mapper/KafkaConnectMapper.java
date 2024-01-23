package io.kafbat.ui.mapper;

import io.kafbat.ui.connect.model.ConnectorStatusConnector;
import io.kafbat.ui.connect.model.ConnectorTask;
import io.kafbat.ui.connect.model.NewConnector;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TaskStatusDTO;
import io.kafbat.ui.model.connect.InternalConnectInfo;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KafkaConnectMapper {
  NewConnector toClient(io.kafbat.ui.model.NewConnectorDTO newConnector);

  ConnectorDTO fromClient(io.kafbat.ui.connect.model.Connector connector);

  ConnectorStatusDTO fromClient(ConnectorStatusConnector connectorStatus);

  TaskDTO fromClient(ConnectorTask connectorTask);

  TaskStatusDTO fromClient(io.kafbat.ui.connect.model.TaskStatus taskStatus);

  ConnectorPluginDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPlugin connectorPlugin);

  ConnectorPluginConfigValidationResponseDTO fromClient(
      io.kafbat.ui.connect.model.ConnectorPluginConfigValidationResponse
          connectorPluginConfigValidationResponse);

  default FullConnectorInfoDTO fullConnectorInfo(InternalConnectInfo connectInfo) {
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
