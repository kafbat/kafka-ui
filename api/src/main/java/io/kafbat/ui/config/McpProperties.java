package io.kafbat.ui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mcp")
public class McpProperties {

  private boolean enabled = false;

  /**
   * Which MCP transport(s) to expose. Streamable HTTP is the current MCP spec
   * transport (single {@code /mcp} endpoint); SSE is the deprecated legacy
   * transport ({@code /mcp/sse} + {@code /mcp/message}) kept for backwards
   * compatibility. Defaults to {@link Transport#BOTH}.
   */
  private Transport transport = Transport.BOTH;

  public enum Transport {
    SSE,
    STREAMABLE,
    BOTH;

    public boolean sseEnabled() {
      return this == SSE || this == BOTH;
    }

    public boolean streamableEnabled() {
      return this == STREAMABLE || this == BOTH;
    }
  }
}
