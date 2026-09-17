package io.kafbat.ui.config;

import io.kafbat.ui.service.mcp.McpSpecificationGenerator;
import io.kafbat.ui.service.mcp.McpTool;
import io.kafbat.ui.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "mcp.enabled", havingValue = "true")
public class McpConfig {

  private final List<McpTool> mcpTools;
  private final McpSpecificationGenerator mcpSpecificationGenerator;

  // Streamable HTTP transport (replaces legacy SSE transport)
  @Bean
  public WebFluxStreamableServerTransportProvider streamableServerTransport() {
    return WebFluxStreamableServerTransportProvider.builder()
        .messageEndpoint("/mcp")
        .build();
  }

  // Router function for Streamable HTTP transport used by Spring WebFlux
  @Bean
  public RouterFunction<?> mcpRouterFunction(WebFluxStreamableServerTransportProvider transport) {
    return transport.getRouterFunction();
  }

  @Bean
  public McpAsyncServer mcpServer(WebFluxStreamableServerTransportProvider transport) {

    // Configure server capabilities with resource support
    var capabilities = McpSchema.ServerCapabilities.builder()
        .resources(false, true)
        .tools(true) // Tools support with list changes notifications
        .prompts(false) // Prompt support with list changes notifications
        .logging() // Logging support
        .build();

    // Create the server with both tools and resource capabilities
    return McpServer.async(transport)
        .serverInfo("Kafka UI MCP", "0.0.1")
        .capabilities(capabilities)
        .tools(tools())
        .build();
  }

  private List<AsyncToolSpecification> tools() {
    List<AsyncToolSpecification> tools = new ArrayList<>();
    for (McpTool mcpTool : mcpTools) {
      tools.addAll(mcpSpecificationGenerator.convertTool(mcpTool));
    }
    return tools;
  }
}
