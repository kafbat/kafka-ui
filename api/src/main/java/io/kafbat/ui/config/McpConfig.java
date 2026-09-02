package io.kafbat.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.service.mcp.McpSpecificationGenerator;
import io.kafbat.ui.service.mcp.McpTool;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(value = "mcp.enabled", havingValue = "true")
public class McpConfig {

  // Legacy SSE transport (deprecated in MCP spec 2025-06-18): two endpoints.
  private static final String SSE_MESSAGE_ENDPOINT = "/mcp/message";
  private static final String SSE_ENDPOINT = "/mcp/sse";
  // Streamable HTTP transport (current MCP spec): a single endpoint.
  private static final String STREAMABLE_ENDPOINT = "/mcp";

  private static final String SSE_ENABLED =
      "T(java.lang.String).valueOf('${mcp.transport:BOTH}').equalsIgnoreCase('SSE') "
      + "|| T(java.lang.String).valueOf('${mcp.transport:BOTH}').equalsIgnoreCase('BOTH')";
  private static final String STREAMABLE_ENABLED =
      "T(java.lang.String).valueOf('${mcp.transport:BOTH}').equalsIgnoreCase('STREAMABLE') "
      + "|| T(java.lang.String).valueOf('${mcp.transport:BOTH}').equalsIgnoreCase('BOTH')";

  private final List<McpTool> mcpTools;
  private final McpSpecificationGenerator mcpSpecificationGenerator;

  // The MCP SDK (0.18.x) abstracts JSON handling behind McpJsonMapper; reuse the
  // application's configured Jackson ObjectMapper.
  @Bean
  public McpJsonMapper mcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  // ---------------------------------------------------------------------------
  // Legacy SSE transport
  // ---------------------------------------------------------------------------

  @Bean
  @ConditionalOnExpression(SSE_ENABLED)
  public WebFluxSseServerTransportProvider sseServerTransport(McpJsonMapper jsonMapper) {
    return WebFluxSseServerTransportProvider.builder()
        .jsonMapper(jsonMapper)
        .messageEndpoint(SSE_MESSAGE_ENDPOINT)
        .sseEndpoint(SSE_ENDPOINT)
        .build();
  }

  @Bean
  @ConditionalOnBean(WebFluxSseServerTransportProvider.class)
  public RouterFunction<?> mcpSseRouterFunction(WebFluxSseServerTransportProvider transport) {
    return transport.getRouterFunction();
  }

  @Bean
  @ConditionalOnBean(WebFluxSseServerTransportProvider.class)
  public McpAsyncServer mcpSseServer(WebFluxSseServerTransportProvider transport) {
    return McpServer.async(transport)
        .serverInfo("Kafka UI MCP", "0.0.1")
        .capabilities(capabilities())
        .tools(tools())
        .build();
  }

  // ---------------------------------------------------------------------------
  // Streamable HTTP transport
  // ---------------------------------------------------------------------------

  @Bean
  @ConditionalOnExpression(STREAMABLE_ENABLED)
  public WebFluxStreamableServerTransportProvider streamableServerTransport(McpJsonMapper jsonMapper) {
    return WebFluxStreamableServerTransportProvider.builder()
        .jsonMapper(jsonMapper)
        .messageEndpoint(STREAMABLE_ENDPOINT)
        .build();
  }

  @Bean
  @ConditionalOnBean(WebFluxStreamableServerTransportProvider.class)
  public RouterFunction<?> mcpStreamableRouterFunction(WebFluxStreamableServerTransportProvider transport) {
    return transport.getRouterFunction();
  }

  @Bean
  @ConditionalOnBean(WebFluxStreamableServerTransportProvider.class)
  public McpAsyncServer mcpStreamableServer(WebFluxStreamableServerTransportProvider transport) {
    return McpServer.async(transport)
        .serverInfo("Kafka UI MCP", "0.0.1")
        .capabilities(capabilities())
        .tools(tools())
        .build();
  }

  // ---------------------------------------------------------------------------

  private McpSchema.ServerCapabilities capabilities() {
    return McpSchema.ServerCapabilities.builder()
        .resources(false, true) // Resource support with list changes notifications
        .tools(true) // Tools support with list changes notifications
        .prompts(false) // Prompt support with list changes notifications
        .logging() // Logging support
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
