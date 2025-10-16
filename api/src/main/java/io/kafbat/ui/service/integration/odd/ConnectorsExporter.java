package io.kafbat.ui.service.integration.odd;

import io.kafbat.ui.connect.model.Connector;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.KafkaConnectService;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opendatadiscovery.client.model.DataEntity;
import org.opendatadiscovery.client.model.DataEntityList;
import org.opendatadiscovery.client.model.DataEntityType;
import org.opendatadiscovery.client.model.DataSource;
import org.opendatadiscovery.client.model.DataTransformer;
import org.opendatadiscovery.client.model.MetadataExtension;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
class ConnectorsExporter {

  private final KafkaConnectService kafkaConnectService;

  Flux<DataEntityList> export(KafkaCluster cluster) {
    return kafkaConnectService.getConnects(cluster, false)
        .flatMap(connect -> kafkaConnectService.getConnectorsWithErrorsSuppress(cluster, connect.getName())
            .flatMapMany(connectors ->
                Flux.fromIterable(connectors.entrySet()).flatMap(e ->
                    kafkaConnectService.getConnectorTopics(cluster, connect.getName(), e.getKey())
                        .map(topics -> createConnectorDataEntity(cluster, connect, e.getValue(), topics)))
            ).buffer(100)
            .map(connectDataEntities -> {
              String dsOddrn = Oddrn.connectDataSourceOddrn(connect.getAddress());
              return new DataEntityList()
                  .dataSourceOddrn(dsOddrn)
                  .items(connectDataEntities);
            })
        );
  }

  Flux<DataSource> getConnectDataSources(KafkaCluster cluster) {
    return kafkaConnectService.getConnects(cluster, false)
        .map(ConnectorsExporter::toDataSource);
  }

  private static DataSource toDataSource(ConnectDTO connect) {
    return new DataSource()
        .oddrn(Oddrn.connectDataSourceOddrn(connect.getAddress()))
        .name(connect.getName())
        .description("Kafka Connect");
  }

  private static DataEntity createConnectorDataEntity(KafkaCluster cluster,
                                                      ConnectDTO connect,
                                                      ExpandedConnector connector,
                                                      ConnectorTopics connectorTopics) {
    Connector connectorInfo = connector.getInfo();
    var metadata = new HashMap<>(extractMetadata(connector));
    metadata.put("type", connectorInfo.getType().name());

    var info = extractConnectorInfo(cluster, connector, connectorTopics);
    DataTransformer transformer = new DataTransformer();
    transformer.setInputs(info.inputs());
    transformer.setOutputs(info.outputs());

    return new DataEntity()
        .oddrn(Oddrn.connectorOddrn(connect.getAddress(), connectorInfo.getName()))
        .name(connectorInfo.getName())
        .description("Kafka Connector \"%s\" (%s)".formatted(connectorInfo.getName(), connectorInfo.getType()))
        .type(DataEntityType.JOB)
        .dataTransformer(transformer)
        .metadata(List.of(
            new MetadataExtension()
                .schemaUrl(URI.create("wontbeused.oops"))
                .metadata(metadata)));
  }

  private static Map<String, Object> extractMetadata(ExpandedConnector connector) {
    // will be sanitized by KafkaConfigSanitizer (if it's enabled)
    return connector.getInfo().getConfig();
  }

  private static ConnectorInfo extractConnectorInfo(KafkaCluster cluster,
                                                    ExpandedConnector connector,
                                                    ConnectorTopics topics) {
    return ConnectorInfo.extract(
        (String) connector.getInfo().getConfig().get("connector.class"),
        connector.getInfo().getType(),
        connector.getInfo().getConfig(),
        topics.getTopics(),
        topic -> Oddrn.topicOddrn(cluster, topic)
    );
  }

}
