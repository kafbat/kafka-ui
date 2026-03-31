package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import java.util.Collection;
import java.util.List;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class KafkaConnectNgramFilter extends NgramFilter<FullConnectorInfoDTO> {
  private final List<Tuple2<List<String>, FullConnectorInfoDTO>> connectors;

  public KafkaConnectNgramFilter(Collection<FullConnectorInfoDTO> connectors) {
    this(connectors, true, new ClustersProperties.NgramProperties(1, 4, true));
  }

  public KafkaConnectNgramFilter(
      Collection<FullConnectorInfoDTO> connectors,
      boolean enabled,
      ClustersProperties.NgramProperties properties) {
    super(properties, enabled);
    this.connectors = connectors.stream().map(this::getItem).toList();
  }

  private Tuple2<List<String>, FullConnectorInfoDTO> getItem(FullConnectorInfoDTO connector) {
    return Tuples.of(
        List.of(
            connector.getName(),
            connector.getConnect(),
            connector.getStatus().getState().getValue(),
            connector.getType().getValue()
        ), connector
    );
  }

  @Override
  protected List<Tuple2<List<String>, FullConnectorInfoDTO>> getItems() {
    return this.connectors;
  }
}
