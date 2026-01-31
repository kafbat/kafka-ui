package io.kafbat.ui.service.integration.odd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.connect.model.Connector;
import io.kafbat.ui.connect.model.ConnectorStatus;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorTypeDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.KafkaConnectService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendatadiscovery.client.model.DataEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConnectorsExporterTest {

  private static final KafkaCluster CLUSTER = KafkaCluster.builder()
      .name("test cluster")
      .bootstrapServers("localhost:9092")
      .build();

  private final KafkaConnectService kafkaConnectService = mock(KafkaConnectService.class);
  private final ConnectorsExporter exporter = new ConnectorsExporter(kafkaConnectService);

  @Test
  void exportsConnectorsAsDataTransformers() {
    ConnectDTO connect = new ConnectDTO();
    connect.setName("testConnect");
    connect.setAddress("http://kconnect:8083");

    ConnectorDTO sinkConnectorDto = new ConnectorDTO();
    sinkConnectorDto.setName("testSink");
    sinkConnectorDto.setType(ConnectorTypeDTO.SINK);
    sinkConnectorDto.setConnect(connect.getName());
    sinkConnectorDto.setConfig(
        Map.of(
            "connector.class", "FileStreamSink",
            "file", "filePathHere",
            "topic", "inputTopic"
        )
    );

    ConnectorDTO sourceConnectorDto = new ConnectorDTO();
    sourceConnectorDto.setName("testSource");
    sourceConnectorDto.setConnect(connect.getName());
    sourceConnectorDto.setType(ConnectorTypeDTO.SOURCE);
    sourceConnectorDto.setConfig(
        Map.of(
            "connector.class", "FileStreamSource",
            "file", "filePathHere",
            "topic", "outputTopic"
        )
    );

    ExpandedConnector sinkConnector = new ExpandedConnector()
        .status(
            new ConnectorStatus()
                .name(sinkConnectorDto.getName())
                .tasks(List.of())
        )
        .info(
            new Connector()
                .name(sinkConnectorDto.getName())
                .type(Connector.TypeEnum.SINK)
                .config(sinkConnectorDto.getConfig())
                .tasks(List.of())
        );

    ExpandedConnector sourceConnector = new ExpandedConnector()
        .status(
            new ConnectorStatus()
                .name(sourceConnectorDto.getName())
                .tasks(List.of())
        )
        .info(
            new Connector()
                .name(sourceConnectorDto.getName())
                .type(Connector.TypeEnum.SOURCE)
                .config(sourceConnectorDto.getConfig())
                .tasks(List.of())
        );

    Map<String, ExpandedConnector> connectors = Map.of(
        sinkConnectorDto.getName(), sinkConnector,
        sourceConnectorDto.getName(), sourceConnector
    );

    when(kafkaConnectService.getConnects(CLUSTER, false))
        .thenReturn(Flux.just(connect));

    when(kafkaConnectService.getConnectorsWithErrorsSuppress(CLUSTER, connect.getName()))
        .thenReturn(Mono.just(connectors));

    when(kafkaConnectService.getConnectorTopics(CLUSTER, connect.getName(), sourceConnectorDto.getName()))
        .thenReturn(Mono.just(new ConnectorTopics().topics(List.of("outputTopic"))));

    when(kafkaConnectService.getConnectorTopics(CLUSTER, connect.getName(), sinkConnectorDto.getName()))
        .thenReturn(Mono.just(new ConnectorTopics().topics(List.of("inputTopic"))));

    StepVerifier.create(exporter.export(CLUSTER))
        .assertNext(dataEntityList -> {
          assertThat(dataEntityList.getDataSourceOddrn())
              .isEqualTo("//kafkaconnect/host/kconnect:8083");

          assertThat(dataEntityList.getItems())
              .hasSize(2);

          assertThat(dataEntityList.getItems())
              .filteredOn(DataEntity::getOddrn, "//kafkaconnect/host/kconnect:8083/connectors/testSink")
              .singleElement()
              .satisfies(sink -> {
                assertThat(sink.getMetadata()).isNotNull();
                assertThat(sink.getDataTransformer()).isNotNull();
                assertThat(sink.getMetadata().get(0).getMetadata())
                    .containsOnlyKeys("type", "connector.class", "file", "topic");
                assertThat(sink.getDataTransformer().getInputs()).contains(
                    "//kafka/cluster/localhost:9092/topics/inputTopic");
              });

          assertThat(dataEntityList.getItems())
              .filteredOn(DataEntity::getOddrn, "//kafkaconnect/host/kconnect:8083/connectors/testSource")
              .singleElement()
              .satisfies(source -> {
                assertThat(source.getMetadata()).isNotNull();
                assertThat(source.getDataTransformer()).isNotNull();
                assertThat(source.getMetadata().get(0).getMetadata())
                    .containsOnlyKeys("type", "connector.class", "file", "topic");
                assertThat(source.getDataTransformer().getOutputs()).contains(
                    "//kafka/cluster/localhost:9092/topics/outputTopic");
              });

        })
        .verifyComplete();
  }

}
