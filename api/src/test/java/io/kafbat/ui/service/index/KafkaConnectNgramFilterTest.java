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
import org.openapitools.jackson.nullable.JsonNullable;

class KafkaConnectNgramFilterTest extends AbstractNgramFilterTest<FullConnectorInfoDTO> {

  @Override
  protected NgramFilter<FullConnectorInfoDTO> buildFilter(List<FullConnectorInfoDTO> items,
      boolean enabled,
      ClustersProperties.NgramProperties ngramProperties) {
    return new KafkaConnectNgramFilter(items, enabled, ngramProperties);
  }

  @Override
  protected List<FullConnectorInfoDTO> items() {
    return IntStream.range(0, 100).mapToObj(i -> new FullConnectorInfoDTO(
        "connect-" + i,
        "connector-" + i,
        "class",
        ConnectorTypeDTO.SINK,
        List.of(),
        new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, "worker-1", "reason"),
        1,
        0,
        JsonNullable.of("connect-connector-"+i))).toList();
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

  @Override
  protected List<FullConnectorInfoDTO> sortedItems() {
    return List.of(
        new FullConnectorInfoDTO(
            "connect-pay",
            "connector-pay",
            "class",
            ConnectorTypeDTO.SINK,
            List.of(),
            new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, null, "reason"),
            1,
            0,
            null
        ),
        new FullConnectorInfoDTO(
            "pay-connect",
            "pay-connector",
            "class",
            ConnectorTypeDTO.SINK,
            List.of(),
            new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, null, "reason"),
            1,
            0,
            null
        )
    );
  }

  @Override
  protected String sortedExample(List<FullConnectorInfoDTO> items) {
    return "pay";
  }

  @Override
  protected List<FullConnectorInfoDTO> sortedResult(List<FullConnectorInfoDTO> items) {
    return List.of(
        new FullConnectorInfoDTO(
            "pay-connect",
            "pay-connector",
            "class",
            ConnectorTypeDTO.SINK,
            List.of(),
            new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, null, "reason"),
            1,
            0, null),
        new FullConnectorInfoDTO(
            "connect-pay",
            "connector-pay",
            "class",
            ConnectorTypeDTO.SINK,
            List.of(),
            new ConnectorStatusDTO(ConnectorStateDTO.RUNNING, null, "reason"),
            1,
            0, null));
  }
}
