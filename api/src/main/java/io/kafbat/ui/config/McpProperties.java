package io.kafbat.ui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MCP (Model Context Protocol) server, bound
 * from the {@code mcp.*} namespace (e.g. {@code mcp.enabled},
 * {@code mcp.transport} / {@code MCP_ENABLED}, {@code MCP_TRANSPORT}).
 */
@Data
@ConfigurationProperties("mcp")
public class McpProperties {

  /**
   * Whether the MCP server is enabled. Defaults to {@code false}.
   */
  private boolean enabled = false;

  /**
   * Which MCP transport(s) to expose. Streamable HTTP is the current MCP spec
   * transport (single {@code /mcp} endpoint); SSE is the deprecated legacy
   * transport ({@code /mcp/sse} + {@code /mcp/message}) kept for backwards
   * compatibility. Defaults to {@link Transport#BOTH}.
   */
  private Transport transport = Transport.BOTH;

  /** The MCP transport modes that can be exposed. */
  public enum Transport {
    /**
     * Legacy HTTP+SSE transport only ({@code /mcp/sse} + {@code /mcp/message}).
     */
    SSE,
    /**
     * Current Streamable HTTP transport only (single {@code /mcp} endpoint).
     */
    STREAMABLE,
    /**
     * Expose both the SSE and Streamable HTTP transports simultaneously.
     */
    BOTH;

    /**
     * Whether the legacy SSE transport should be active for this mode.
     *
     * @return {@code true} for {@link #SSE} and {@link #BOTH}
     */
    public boolean sseEnabled() {
      return this == SSE || this == BOTH;
    }

    /**
     * Whether the Streamable HTTP transport should be active for this mode.
     *
     * @return {@code true} for {@link #STREAMABLE} and {@link #BOTH}
     */
    public boolean streamableEnabled() {
      return this == STREAMABLE || this == BOTH;
    }
  }
}
