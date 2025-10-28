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
