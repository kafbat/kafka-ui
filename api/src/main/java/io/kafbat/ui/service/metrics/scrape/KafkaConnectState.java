package io.kafbat.ui.service.metrics.scrape;

import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTypeDTO;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Builder(toBuilder = true)
@RequiredArgsConstructor
@Value
public class KafkaConnectState {
  Instant scrapeFinishedAt;
  String name;
  String version;
  List<ConnectorState> connectors;

  public record ConnectorState(String name,
                               ConnectorTypeDTO connectorType,
                               ConnectorStatusDTO status,
                               List<String> topics) {}
}
