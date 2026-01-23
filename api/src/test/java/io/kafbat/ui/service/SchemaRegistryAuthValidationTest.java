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
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setSchemaRegistry("http://localhost:8081");

    ClustersProperties.SchemaRegistryAuth basicAuth = new ClustersProperties.SchemaRegistryAuth();
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    cluster.setSchemaRegistryAuth(basicAuth);

    ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
    oauth.setTokenUrl("http://localhost:8080/token");
    oauth.setClientId("client-id");
    oauth.setClientSecret("client-secret");
    cluster.setSchemaRegistryOAuth(oauth);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> factory.create(new ClustersProperties(), cluster)
    );

    assertTrue(exception.getMessage().contains("both basic auth and OAuth are configured"));
  }

  @Test
  void shouldNotThrowExceptionWhenOnlyBasicAuthConfigured() {
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    ClustersProperties.SchemaRegistryAuth basicAuth = new ClustersProperties.SchemaRegistryAuth();
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    cluster.setSchemaRegistryAuth(basicAuth);

    factory.create(new ClustersProperties(), cluster);
  }

  @Test
  void shouldNotThrowExceptionWhenOnlyOAuthConfigured() {
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
    oauth.setTokenUrl("http://localhost:8080/token");
    oauth.setClientId("client-id");
    oauth.setClientSecret("client-secret");
    cluster.setSchemaRegistryOAuth(oauth);

    factory.create(new ClustersProperties(), cluster);
  }

  @Test
  void shouldNotThrowExceptionWhenNeitherAuthConfigured() {
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test");
    cluster.setBootstrapServers("localhost:9092");
    cluster.setSchemaRegistry("http://localhost:8081");

    factory.create(new ClustersProperties(), cluster);
  }
}
