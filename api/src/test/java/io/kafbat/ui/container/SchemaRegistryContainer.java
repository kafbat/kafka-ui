package io.kafbat.ui.container;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.ConfluentKafkaContainer;

public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> {
  private static final int SCHEMA_PORT = 8081;

  public SchemaRegistryContainer(String version) {
    super("confluentinc/cp-schema-registry:" + version);
    withExposedPorts(8081);
  }

  public SchemaRegistryContainer withKafka(ConfluentKafkaContainer kafka) {
    String bootstrapServers = kafka.getNetworkAliases().getFirst() + ":9095";
    return withKafka(kafka.getNetwork(), bootstrapServers);
  }

  public SchemaRegistryContainer withKafka(Network network, String bootstrapServers) {
    withNetwork(network);
    withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
    withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_PORT);
    withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + bootstrapServers);
    return self();
  }

  public String getUrl() {
    return "http://" + getHost() + ":" + getMappedPort(SCHEMA_PORT);
  }

  public SchemaRegistryClient schemaRegistryClient() {
    return new CachedSchemaRegistryClient(getUrl(), 1000);
  }

}
