# Descriptions of docker-compose configurations (*.yaml)

1. [kafka-ui.yaml](./kafbat-ui.yaml) - Default configuration with 2 kafka clusters with two nodes of Schema Registry, one kafka-connect and a few dummy topics.
2. [kafka-ui-arm64.yaml](../../.dev/dev_arm64.yaml) - Default configuration for ARM64(Mac M1) architecture with 1 kafka cluster without zookeeper with one node of Schema Registry, one kafka-connect and a few dummy topics.
3. [kafka-ui-ssl.yml](./kafka-ssl.yml) - Connect to Kafka via TLS/SSL
4. [kafka-cluster-sr-auth.yaml](./cluster-sr-auth.yaml) - Schema registry with authentication.
5. [kafka-ui-auth-context.yaml](./auth-context.yaml) - Basic (username/password) authentication with custom path (URL) (issue 861).
6. [e2e-tests.yaml](./e2e-tests.yaml) - Configuration with different connectors (github-source, s3, sink-activities, source-activities) and Ksql functionality.
7. [kafka-ui-jmx-secured.yml](./ui-jmx-secured.yml) - Kafka’s JMX with SSL and authentication.
8. [kafka-ui-reverse-proxy.yaml](./nginx-proxy.yaml) - An example for using the app behind a proxy (like nginx).
9. [kafka-ui-sasl.yaml](./ui-sasl.yaml) - SASL auth for Kafka.
10. [kafka-ui-traefik-proxy.yaml](./traefik-proxy.yaml) - Traefik specific proxy configuration.
11. [kafka-ui-with-jmx-exporter.yaml](./ui-with-jmx-exporter.yaml) - A configuration with 2 kafka clusters with enabled prometheus jmx exporters instead of jmx.
12. [kafka-with-zookeeper.yaml](./kafka-zookeeper.yaml) - An example for using kafka with zookeeper