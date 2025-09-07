package io.kafbat.ui.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.model.ClusterInfo;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TaskIdDTO;
import io.kafbat.ui.model.TaskStatusDTO;
import io.kafbat.ui.model.connect.InternalConnectorInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

class KafkaConnectMapperTest {

  @Test
  void toKafkaConnect() {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    List<InternalConnectorInfo> connectors = new ArrayList<>();
    int failedConnectors = 0;
    int failedTasks = 0;
    int tasksPerConnector = random.nextInt(1, 10);

    for (int i = 0; i < 10; i++) {
      ConnectorStateDTO connectorState;
      if (random.nextBoolean()) {
        connectorState = ConnectorStateDTO.FAILED;
        failedConnectors++;
      } else {
        connectorState = ConnectorStateDTO.RUNNING;
      }

      ConnectorDTO connectorDto = new ConnectorDTO();
      connectorDto.setName(UUID.randomUUID().toString());

      String traceMessage = connectorState == ConnectorStateDTO.FAILED
          ? "Test error trace for failed connector"
          : null;

      connectorDto.setStatus(
          new ConnectorStatusDTO(connectorState, UUID.randomUUID().toString(), traceMessage)
      );

      List<TaskDTO> tasks = new ArrayList<>();
      List<TaskIdDTO> taskIds = new ArrayList<>();

      for (int j = 0; j < tasksPerConnector; j++) {
        TaskDTO task = new TaskDTO();
        TaskIdDTO taskId = new TaskIdDTO(UUID.randomUUID().toString(), j);
        task.setId(taskId);

        ConnectorTaskStatusDTO state;
        if (random.nextBoolean()) {
          state = ConnectorTaskStatusDTO.FAILED;
          failedTasks++;
        } else {
          state = ConnectorTaskStatusDTO.RUNNING;
        }

        TaskStatusDTO status = new TaskStatusDTO();
        status.setState(state);
        task.setStatus(status);
        tasks.add(task);
        taskIds.add(taskId);
      }

      connectorDto.setTasks(taskIds);
      InternalConnectorInfo connector = InternalConnectorInfo.builder()
          .connector(connectorDto)
          .tasks(tasks)
          .build();

      connectors.add(connector);
    }

    ClusterInfo clusterInfo = new ClusterInfo();
    clusterInfo.setVersion(UUID.randomUUID().toString());
    clusterInfo.setCommit(UUID.randomUUID().toString());
    clusterInfo.setKafkaClusterId(UUID.randomUUID().toString());

    ClustersProperties.ConnectCluster connectCluster = ClustersProperties.ConnectCluster.builder()
        .name(UUID.randomUUID().toString())
        .address("http://localhost:" + random.nextInt(1000, 5000))
        .username(UUID.randomUUID().toString())
        .password(UUID.randomUUID().toString()).build();

    ConnectDTO connectDto = new ConnectDTO();
    connectDto.setName(connectCluster.getName());
    connectDto.setAddress(connectCluster.getAddress());
    connectDto.setVersion(JsonNullable.of(clusterInfo.getVersion()));
    connectDto.setCommit(JsonNullable.of(clusterInfo.getCommit()));
    connectDto.setClusterId(JsonNullable.of(clusterInfo.getKafkaClusterId()));
    connectDto.setConnectorsCount(JsonNullable.of(connectors.size()));
    connectDto.setFailedConnectorsCount(JsonNullable.of(failedConnectors));
    connectDto.setTasksCount(JsonNullable.of(connectors.size() * tasksPerConnector));
    connectDto.setFailedTasksCount(JsonNullable.of(failedTasks));

    KafkaConnectMapper mapper = new KafkaConnectMapperImpl();
    ConnectDTO kafkaConnect = mapper.toKafkaConnect(
        connectCluster,
        connectors,
        clusterInfo,
        true
    );

    assertThat(kafkaConnect).isNotNull();
    assertThat(kafkaConnect).isEqualTo(connectDto);

  }
}
