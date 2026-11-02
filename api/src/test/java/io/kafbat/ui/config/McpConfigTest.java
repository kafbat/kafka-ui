package io.kafbat.ui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.service.mcp.McpSpecificationGenerator;
import io.kafbat.ui.service.mcp.McpTool;
import java.util.List;
import org.junit.jupiter.api.Test;

class McpConfigTest {

  @Test
  void createsSseTransportRouterAndServer() {
    McpTool tool = mock(McpTool.class);
    McpSpecificationGenerator generator = mock(McpSpecificationGenerator.class);
    when(generator.convertTool(tool)).thenReturn(List.of());
    McpConfig config = new McpConfig(List.of(tool), generator);

    var transport = config.sseServerTransport(new ObjectMapper());
    assertThat(transport).isNotNull();
    assertThat(config.mcpRouterFunction(transport)).isNotNull();

    var server = config.mcpServer(transport);
    assertThat(server).isNotNull();
    assertThat(server.getServerCapabilities().tools()).isNotNull();
    server.close();
  }
}
