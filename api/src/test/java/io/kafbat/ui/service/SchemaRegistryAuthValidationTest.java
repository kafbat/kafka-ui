package io.kafbat.ui.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.config.WebclientProperties;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsRetriever;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class SchemaRegistryAuthValidationTest {

  private KafkaClusterFactory factory;

  @BeforeEach
  void setUp() {
    WebclientProperties webclientProperties = new WebclientProperties();
    JmxMetricsRetriever jmxMetricsRetriever = Mockito.mock(JmxMetricsRetriever.class);
    factory = new KafkaClusterFactory(webclientProperties, jmxMetricsRetriever);
  }

  @Test
  void shouldThrowExceptionWhenBothBasicAuthAndOAuthConfigured() {
    // Given
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setSchemaRegistry("http://localhost:8081");

    // Configure basic auth
    ClustersProperties.SchemaRegistryAuth basicAuth = new ClustersProperties.SchemaRegistryAuth();
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    cluster.setSchemaRegistryAuth(basicAuth);

    // Configure OAuth
    ClustersProperties.SchemaRegistryOauth oauth = new ClustersProperties.SchemaRegistryOauth();
    oauth.setTokenUrl("http://localhost:8080/token");
    oauth.setClientId("client-id");
    oauth.setClientSecret("client-secret");
    cluster.setSchemaRegistryOauth(oauth);

    // When/Then
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> factory.create(new ClustersProperties(), cluster)
    );

    assertTrue(exception.getMessage().contains("both basic auth and OAuth are configured"));
  }

  @Test
  void shouldNotThrowExceptionWhenOnlyBasicAuthConfigured() {
    // Given
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    // Configure only basic auth
    ClustersProperties.SchemaRegistryAuth basicAuth = new ClustersProperties.SchemaRegistryAuth();
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    cluster.setSchemaRegistryAuth(basicAuth);

    // When/Then - should not throw
    factory.create(new ClustersProperties(), cluster);
  }

  @Test
  void shouldNotThrowExceptionWhenOnlyOAuthConfigured() {
    // Given
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    // Configure only OAuth
    ClustersProperties.SchemaRegistryOauth oauth = new ClustersProperties.SchemaRegistryOauth();
    oauth.setTokenUrl("http://localhost:8080/token");
    oauth.setClientId("client-id");
    oauth.setClientSecret("client-secret");
    cluster.setSchemaRegistryOauth(oauth);

    // When/Then - should not throw
    factory.create(new ClustersProperties(), cluster);
  }

  @Test
  void shouldNotThrowExceptionWhenNeitherAuthConfigured() {
    // Given
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    // When/Then - should not throw
    factory.create(new ClustersProperties(), cluster);
  }
}
