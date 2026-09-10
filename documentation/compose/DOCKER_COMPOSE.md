# Descriptions of docker-compose configurations (*.yaml)

1. [kafka-ui.yaml](./kafbat-ui.yaml) - Default configuration with 2 Kafka clusters with two nodes of Schema Registry, one Kafka Connect, and a few dummy topics.
2. [kafka-ui-ssl.yml](./kafka-ssl.yml) - Connect to Kafka via TLS/SSL.
3. [kafka-cluster-sr-auth.yaml](./cluster-sr-auth.yaml) - Schema Registry with authentication.
4. [kafka-ui-auth-context.yaml](./auth-context.yaml) - Basic (username/password) authentication with custom path (URL) (issue 861).
5. [e2e-tests.yaml](./e2e-tests.yaml) - Configuration with different connectors (github-source, s3, sink-activities, source-activities) and KSQL functionality.
6. [kafka-ui-jmx-secured.yml](./ui-jmx-secured.yml) - Kafka's JMX with SSL and authentication.
7. [kafka-ui-reverse-proxy.yaml](./nginx-proxy.yaml) - An example of using the app behind a proxy (like nginx).
8. [kafka-ui-sasl.yaml](./ui-sasl.yaml) - SASL authentication for Kafka.
9. [kafka-ui-traefik-proxy.yaml](./traefik-proxy.yaml) - Traefik-specific proxy configuration.
10. [kafka-ui-with-jmx-exporter.yaml](./ui-with-jmx-exporter.yaml) - A configuration with 2 Kafka clusters with enabled Prometheus JMX exporters instead of JMX.
11. [kafka-with-zookeeper.yaml](./kafka-zookeeper.yaml) - An example of using Kafka with ZooKeeper.
12. [kafbat-ui-mcp.yaml](./kafbat-ui-mcp.yaml) - Enables the MCP (Model Context Protocol) server for AI clients. `MCP_ENABLED` / `MCP_TRANSPORT` are ordinary Spring Boot properties, so they can be set either as environment variables (as in the compose example) or via a config file:

    | Property | Environment variable | Values (default) |
    | --- | --- | --- |
    | `mcp.enabled` | `MCP_ENABLED` | `true` / `false` (`false`) |
    | `mcp.transport` | `MCP_TRANSPORT` | `STREAMABLE` \| `SSE` \| `BOTH` (`BOTH`) |

    YAML equivalent of the compose example:

    ```yaml
    mcp:
      enabled: true
      transport: STREAMABLE   # STREAMABLE | SSE | BOTH (case-insensitive)
    ```

    `STREAMABLE` exposes a single `/mcp` endpoint (current MCP spec); `SSE` exposes the deprecated legacy `/mcp/sse` + `/mcp/message` endpoints.
