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

/**
 * Spring configuration that wires up the Model Context Protocol (MCP) server.
 *
 * <p>Only active when {@code mcp.enabled=true}. Two transports can be exposed,
 * selected by the {@code mcp.transport} property
 * ({@link McpProperties.Transport}):
 * <ul>
 *   <li>the legacy HTTP+SSE transport ({@code /mcp/sse} + {@code /mcp/message}),
 *       deprecated in the MCP spec (2025-06-18); and</li>
 *   <li>the current Streamable HTTP transport (a single {@code /mcp} endpoint).</li>
 * </ul>
 * Each active transport is backed by its own {@link McpAsyncServer} exposing the
 * same set of tools, and its {@link RouterFunction} is picked up by Spring
 * WebFlux.
 */
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

  /**
   * Adapts the application's Jackson {@link ObjectMapper} to the SDK's
   * {@link McpJsonMapper} abstraction (introduced in mcp-spring-webflux 0.18.x),
   * so MCP JSON (de)serialization uses the same configuration as the rest of the
   * app.
   *
   * @param mapper the shared application {@link ObjectMapper}
   * @return an {@link McpJsonMapper} backed by {@code mapper}
   */
  @Bean
  public McpJsonMapper mcpJsonMapper(ObjectMapper mapper) {
    return new JacksonMcpJsonMapper(mapper);
  }

  // ---------------------------------------------------------------------------
  // Legacy SSE transport
  // ---------------------------------------------------------------------------

  /**
   * Creates the legacy HTTP+SSE transport provider (endpoints {@code /mcp/sse}
   * and {@code /mcp/message}). Only registered when {@code mcp.transport} is
   * {@code SSE} or {@code BOTH}.
   *
   * @param jsonMapper the MCP JSON mapper
   * @return the SSE transport provider
   */
  @Bean
  @ConditionalOnExpression(SSE_ENABLED)
  public WebFluxSseServerTransportProvider sseServerTransport(McpJsonMapper jsonMapper) {
    return WebFluxSseServerTransportProvider.builder()
        .jsonMapper(jsonMapper)
        .messageEndpoint(SSE_MESSAGE_ENDPOINT)
        .sseEndpoint(SSE_ENDPOINT)
        .build();
  }

  /**
   * Exposes the SSE transport's routes to Spring WebFlux. Only present when the
   * SSE transport provider bean exists.
   *
   * @param transport the SSE transport provider
   * @return the router function serving the SSE endpoints
   */
  @Bean
  @ConditionalOnBean(WebFluxSseServerTransportProvider.class)
  public RouterFunction<?> mcpSseRouterFunction(WebFluxSseServerTransportProvider transport) {
    return transport.getRouterFunction();
  }

  /**
   * Builds the {@link McpAsyncServer} bound to the SSE transport. Only present
   * when the SSE transport provider bean exists.
   *
   * @param transport the SSE transport provider
   * @return the MCP async server for the SSE transport
   */
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

  /**
   * Creates the current Streamable HTTP transport provider (single {@code /mcp}
   * endpoint). Only registered when {@code mcp.transport} is {@code STREAMABLE}
   * or {@code BOTH}.
   *
   * @param jsonMapper the MCP JSON mapper
   * @return the Streamable HTTP transport provider
   */
  @Bean
  @ConditionalOnExpression(STREAMABLE_ENABLED)
  public WebFluxStreamableServerTransportProvider streamableServerTransport(McpJsonMapper jsonMapper) {
    return WebFluxStreamableServerTransportProvider.builder()
        .jsonMapper(jsonMapper)
        .messageEndpoint(STREAMABLE_ENDPOINT)
        .build();
  }

  /**
   * Exposes the Streamable HTTP transport's routes to Spring WebFlux. Only
   * present when the Streamable HTTP transport provider bean exists.
   *
   * @param transport the Streamable HTTP transport provider
   * @return the router function serving the {@code /mcp} endpoint
   */
  @Bean
  @ConditionalOnBean(WebFluxStreamableServerTransportProvider.class)
  public RouterFunction<?> mcpStreamableRouterFunction(WebFluxStreamableServerTransportProvider transport) {
    return transport.getRouterFunction();
  }

  /**
   * Builds the {@link McpAsyncServer} bound to the Streamable HTTP transport.
   * Only present when the Streamable HTTP transport provider bean exists.
   *
   * @param transport the Streamable HTTP transport provider
   * @return the MCP async server for the Streamable HTTP transport
   */
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

  /**
   * Declares the MCP server capabilities shared by every transport: tools (with
   * list-change notifications), read-only resources, and logging; prompts are
   * disabled.
   *
   * @return the configured {@link McpSchema.ServerCapabilities}
   */
  private McpSchema.ServerCapabilities capabilities() {
    return McpSchema.ServerCapabilities.builder()
        .resources(false, true) // Resource support with list changes notifications
        .tools(true) // Tools support with list changes notifications
        .prompts(false) // Prompt support with list changes notifications
        .logging() // Logging support
        .build();
  }

  /**
   * Collects the tool specifications exposed to MCP clients by converting every
   * {@link McpTool} bean via {@link McpSpecificationGenerator}.
   *
   * @return the list of tool specifications
   */
  private List<AsyncToolSpecification> tools() {
    List<AsyncToolSpecification> tools = new ArrayList<>();
    for (McpTool mcpTool : mcpTools) {
      tools.addAll(mcpSpecificationGenerator.convertTool(mcpTool));
    }
    return tools;
  }
}
