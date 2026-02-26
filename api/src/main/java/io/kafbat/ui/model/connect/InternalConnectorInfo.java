package io.kafbat.ui.model.connect;

import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.TaskDTO;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class InternalConnectorInfo {
  private final ConnectorDTO connector;
  private final Map<String, Object> config;
  private final List<TaskDTO> tasks;
  private final List<String> topics;
  private final String consumer;
}
