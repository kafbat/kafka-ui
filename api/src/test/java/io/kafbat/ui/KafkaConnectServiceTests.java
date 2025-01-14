package io.kafbat.ui;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.kafbat.ui.api.model.ErrorResponse;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorPluginConfigDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValidationResponseDTO;
import io.kafbat.ui.model.ConnectorPluginConfigValueDTO;
import io.kafbat.ui.model.ConnectorPluginDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTypeDTO;
import io.kafbat.ui.model.NewConnectorDTO;
import io.kafbat.ui.model.TaskIdDTO;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

@Slf4j
public class KafkaConnectServiceTests extends AbstractIntegrationTest {
  private final String connectName = "kafka-connect";
  private final String connectorName = UUID.randomUUID().toString();
  private final Map<String, Object> config = Map.of(
      "name", connectorName,
      "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
      "tasks.max", "1",
      "topics", "output-topic",
      "file", "/tmp/test",
      "test.password", "******"
  );

  @Autowired
  private WebTestClient webTestClient;


  @BeforeEach
  public void setUp() {

    webTestClient.post()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .bodyValue(new NewConnectorDTO()
            .name(connectorName)
            .config(Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
                "tasks.max", "1",
                "topics", "output-topic",
                "file", "/tmp/test",
                "test.password", "test-credentials")))
        .exchange()
        .expectStatus().isOk();

  }

  @AfterEach
  public void tearDown() {
    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}", LOCAL,
            connectName, connectorName)
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  public void shouldListAllConnectors() {
    webTestClient.get()
            .uri("/api/clusters/{clusterName}/connectors", LOCAL)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath(String.format("$[?(@.name == '%s')]", connectorName))
            .exists();
  }

  @Test
  public void shouldFilterByNameConnectors() {
    webTestClient.get()
            .uri(
                    "/api/clusters/{clusterName}/connectors?search={search}",
                    LOCAL,
                    connectorName.split("-")[1])
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath(String.format("$[?(@.name == '%s')]", connectorName))
            .exists();
  }

  @Test
  public void shouldFilterByStatusConnectors() {
    webTestClient.get()
            .uri(
                    "/api/clusters/{clusterName}/connectors?search={search}",
                    LOCAL,
                    "running")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath(String.format("$[?(@.name == '%s')]", connectorName))
            .exists();
  }

  @Test
  public void shouldFilterByTypeConnectors() {
    webTestClient.get()
            .uri(
                    "/api/clusters/{clusterName}/connectors?search={search}",
                    LOCAL,
                    "sink")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath(String.format("$[?(@.name == '%s')]", connectorName))
            .exists();
  }

  @Test
  public void shouldNotFilterConnectors() {
    webTestClient.get()
            .uri(
                    "/api/clusters/{clusterName}/connectors?search={search}",
                    LOCAL,
                    "something-else")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath(String.format("$[?(@.name == '%s')]", connectorName))
            .doesNotExist();
  }

  @Test
  public void shouldListConnectors() {
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(String.class)
        .contains(connectorName);
  }

  @Test
  public void shouldReturnNotFoundForNonExistingCluster() {
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", "nonExistingCluster",
            connectName)
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  public void shouldReturnNotFoundForNonExistingConnectName() {
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL,
            "nonExistingConnect")
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  public void shouldRetrieveConnector() {
    ConnectorDTO expected = new ConnectorDTO()
        .connect(connectName)
        .status(new ConnectorStatusDTO()
            .state(ConnectorStateDTO.RUNNING)
            .workerId("kafka-connect:8083"))
        .tasks(List.of(new TaskIdDTO()
            .connector(connectorName)
            .task(0)))
        .type(ConnectorTypeDTO.SINK)
        .name(connectorName)
        .config(config);
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}", LOCAL,
            connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorDTO.class)
        .value(connector -> assertEquals(expected, connector));
  }

  @Test
  public void shouldUpdateConfig() {
    webTestClient.put()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .bodyValue(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", "another-topic",
            "file", "/tmp/new"
            )
        )
        .exchange()
        .expectStatus().isOk();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .isEqualTo(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", "another-topic",
            "file", "/tmp/new",
            "name", connectorName
        ));
  }

  @Test
  public void shouldReturn400WhenConnectReturns400ForInvalidConfigCreate() {
    var connectorName = UUID.randomUUID().toString();
    webTestClient.post()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .bodyValue(Map.of(
            "name", connectorName,
            "config", Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
                "tasks.max", "invalid number",
                "topics", "another-topic",
                "file", "/tmp/test"
            ))
        )
        .exchange()
        .expectStatus().isBadRequest();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath(String.format("$[?(@ == '%s')]", connectorName))
        .doesNotExist();
  }

  @Test
  public void shouldReturn400WhenConnectReturns500ForInvalidConfigCreate() {
    var connectorName = UUID.randomUUID().toString();
    webTestClient.post()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .bodyValue(Map.of(
            "name", connectorName,
            "config", Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector"
            ))
        )
        .exchange()
        .expectStatus().isBadRequest();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath(String.format("$[?(@ == '%s')]", connectorName))
        .doesNotExist();
  }


  @Test
  @SuppressWarnings("checkstyle:LineLength")
  public void shouldReturn400WhenConnectReturns400ForInvalidConfigUpdate() {
    webTestClient.put()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .bodyValue(Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
                "tasks.max", "invalid number",
                "topics", "another-topic",
                "file", "/tmp/test"
            )
        )
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ErrorResponse.class)
        .value(response -> assertThat(response.getMessage()).isEqualTo(
            """
                Connector configuration is invalid and contains the following 2 error(s):
                Invalid value invalid number for configuration tasks.max: Not a number of type INT
                Invalid value null for configuration tasks.max: Value must be non-null
                You can also find the above list of errors at the endpoint `/connector-plugins/{connectorType}/config/validate`"""
        ));

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .isEqualTo(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", "output-topic",
            "file", "/tmp/test",
            "name", connectorName,
            "test.password", "******"
        ));
  }

  @Test
  public void shouldReturn400WhenConnectReturns500ForInvalidConfigUpdate() {
    webTestClient.put()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .bodyValue(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector"
            )
        )
        .exchange()
        .expectStatus().isBadRequest();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/config",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .isEqualTo(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", "output-topic",
            "file", "/tmp/test",
            "test.password", "******",
            "name", connectorName
        ));
  }

  @Test
  public void shouldRetrieveConnectorPlugins() {
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/plugins", LOCAL, connectName)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(ConnectorPluginDTO.class)
        .value(plugins -> assertThat(plugins.size()).isGreaterThan(0));
  }

  @Test
  public void shouldSuccessfullyValidateConnectorPluginConfiguration() {
    var pluginName = "FileStreamSinkConnector";
    var path =
        "/api/clusters/{clusterName}/connects/{connectName}/plugins/{pluginName}/config/validate";
    webTestClient.put()
        .uri(path, LOCAL, connectName, pluginName)
        .bodyValue(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "1",
            "topics", "output-topic",
            "file", "/tmp/test",
            "name", connectorName
            )
        )
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorPluginConfigValidationResponseDTO.class)
        .value(response -> assertEquals(0, response.getErrorCount()));
  }

  @Test
  public void shouldValidateAndReturnErrorsOfConnectorPluginConfiguration() {
    var pluginName = "FileStreamSinkConnector";
    var path =
        "/api/clusters/{clusterName}/connects/{connectName}/plugins/{pluginName}/config/validate";
    webTestClient.put()
        .uri(path, LOCAL, connectName, pluginName)
        .bodyValue(Map.of(
            "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
            "tasks.max", "0",
            "topics", "output-topic",
            "file", "/tmp/test",
            "name", connectorName
            )
        )
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorPluginConfigValidationResponseDTO.class)
        .value(response -> {
          assertEquals(1, response.getErrorCount());
          var error = response.getConfigs().stream()
              .map(ConnectorPluginConfigDTO::getValue)
              .map(ConnectorPluginConfigValueDTO::getErrors)
              .filter(not(List::isEmpty))
              .findFirst().orElseThrow();
          assertEquals(
              "Invalid value 0 for configuration tasks.max: Value must be at least 1",
              error.get(0)
          );
        });
  }

  @Test
  public void shouldReturn400WhenTryingToCreateConnectorWithExistingName() {
    webTestClient.post()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors", LOCAL, connectName)
        .bodyValue(new NewConnectorDTO()
            .name(connectorName)
            .config(Map.of(
                "connector.class", "org.apache.kafka.connect.file.FileStreamSinkConnector",
                "tasks.max", "1",
                "topics", "output-topic",
                "file", "/tmp/test"
            ))
        )
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  public void shouldResetConnectorWhenInStoppedState() {

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorDTO.class)
        .value(connector -> assertThat(connector.getStatus().getState()).isEqualTo(ConnectorStateDTO.RUNNING));

    webTestClient.post()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/action/STOP",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorDTO.class)
        .value(connector -> assertThat(connector.getStatus().getState()).isEqualTo(ConnectorStateDTO.STOPPED));

    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/offsets",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk();

  }

  @Test
  public void shouldReturn400WhenResettingConnectorInRunningState() {

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}",
            LOCAL, connectName, connectorName)
        .exchange()
        .expectStatus().isOk()
        .expectBody(ConnectorDTO.class)
        .value(connector -> assertThat(connector.getStatus().getState()).isEqualTo(ConnectorStateDTO.RUNNING));

    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/connects/{connectName}/connectors/{connectorName}/offsets", LOCAL,
            connectName, connectorName)
        .exchange()
        .expectStatus().isBadRequest();

  }
}
