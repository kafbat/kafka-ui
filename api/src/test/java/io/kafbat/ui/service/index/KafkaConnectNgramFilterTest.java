package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTypeDTO;
import io.kafbat.ui.model.FullConnectorInfoDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class KafkaConnectNgramFilterTest extends AbstractNgramFilterTest<FullConnectorInfoDTO> {

  @Override
  protected NgramFilter<FullConnectorInfoDTO> buildFilter(List<FullConnectorInfoDTO> items,
                                                          boolean enabled,
                                                          ClustersProperties.NgramProperties ngramProperties) {
    return new KafkaConnectNgramFilter(items, enabled, ngramProperties);
  }

  @Override
  protected List<FullConnectorInfoDTO> items() {
    return IntStream.range(0, 100).mapToObj(i ->
        new FullConnectorInfoDTO(
            "connect-" + i,
            "connector-" + i,
            "class",
            ConnectorTypeDTO.SINK,
            List.of(),
            new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, "reason"),
            1,
            0
        )
    ).toList();
  }

  @Override
  protected Comparator<FullConnectorInfoDTO> comparator() {
    return Comparator.comparing(FullConnectorInfoDTO::getConnect);
  }

  @Override
  protected Map.Entry<String, FullConnectorInfoDTO> example(List<FullConnectorInfoDTO> items) {
    FullConnectorInfoDTO first = items.getFirst();
    return Map.entry(first.getConnect(), first);
  }
}
