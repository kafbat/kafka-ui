package io.kafbat.ui.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.config.WebclientProperties;
import io.kafbat.ui.exception.ValidationException;
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

    ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
    oauth.setTokenUrl("http://localhost:8080/token");
    oauth.setClientId("client-id");
    oauth.setClientSecret("client-secret");

    ClustersProperties.SchemaRegistryAuth basicAuth = new ClustersProperties.SchemaRegistryAuth();
    basicAuth.setUsername("user");
    basicAuth.setPassword("pass");
    basicAuth.setOauth(oauth);
    cluster.setSchemaRegistryAuth(basicAuth);

    ValidationException exception = assertThrows(
        ValidationException.class,
        () -> factory.create(new ClustersProperties(), cluster)
    );

    assertTrue(exception.getMessage().contains("both basic auth and OAuth are configured"));
  }

  @Test
  void shouldThrowExceptionWhenOAuthIsPartiallyConfigured() {
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setSchemaRegistry("http://localhost:8081");

    ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
    oauth.setTokenUrl("http://localhost:8080/token");

    ClustersProperties.SchemaRegistryAuth auth = new ClustersProperties.SchemaRegistryAuth();
    auth.setOauth(oauth);
    cluster.setSchemaRegistryAuth(auth);

    ValidationException exception = assertThrows(
        ValidationException.class,
        () -> factory.create(new ClustersProperties(), cluster)
    );

    assertTrue(exception.getMessage().contains("one of the OAuth Parameters are missing"));
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

    ClustersProperties.SchemaRegistryAuth auth = new ClustersProperties.SchemaRegistryAuth();
    auth.setOauth(oauth);
    cluster.setSchemaRegistryAuth(auth);

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
