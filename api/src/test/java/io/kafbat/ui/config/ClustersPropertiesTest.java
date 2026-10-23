package io.kafbat.ui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ClustersPropertiesTest {

  private static final Validator VALIDATOR;

  static {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      VALIDATOR = validatorFactory.getValidator();
    }
  }

  @Test
  void clusterNamesShouldBeUniq() {
    ClustersProperties properties = new ClustersProperties();
    var c1 = new ClustersProperties.Cluster();
    c1.setName("test");
    var c2 = new ClustersProperties.Cluster();
    c2.setName("test"); //same name

    Collections.addAll(properties.getClusters(), c1, c2);

    assertThatThrownBy(properties::validateAndSetDefaults)
        .hasMessageContaining("Application config isn't valid");
  }

  @Test
  void clusterNamesShouldSetIfMultipleClustersProvided() {
    ClustersProperties properties = new ClustersProperties();
    var c1 = new ClustersProperties.Cluster();
    c1.setName("test1");
    var c2 = new ClustersProperties.Cluster(); //name not set

    Collections.addAll(properties.getClusters(), c1, c2);

    assertThatThrownBy(properties::validateAndSetDefaults)
        .hasMessageContaining("Application config isn't valid");
  }

  @Test
  void ifOnlyOneClusterProvidedNameIsOptionalAndSetToDefault() {
    ClustersProperties properties = new ClustersProperties();
    properties.getClusters().add(new ClustersProperties.Cluster());

    properties.validateAndSetDefaults();

    assertThat(properties.getClusters())
        .element(0)
        .extracting("name")
        .isEqualTo("Default");
  }

  @Test
  void kafkaConnectConfigHasSensibleDefaults() {
    var config = new ClustersProperties.KafkaConnect();
    assertThat(config.getScrapeConcurrency()).isEqualTo(4);
    assertThat(config.getMaxRetries()).isEqualTo(5);
    assertThat(config.getRetryBaseDelayMs()).isEqualTo(500L);
    assertThat(config.getRetryMaxDelayMs()).isEqualTo(10000L);
  }

  @Test
  void kafkaConnectClientFieldIsInitializedByDefault() {
    ClustersProperties properties = new ClustersProperties();
    assertThat(properties.getKafkaConnectClient()).isNotNull();
    assertThat(properties.getKafkaConnectClient().getScrapeConcurrency()).isEqualTo(4);
  }

  @Test
  void kafkaConnectConfigOverriddenViaYamlStyleProperties() {
    Map<String, String> props = new HashMap<>();
    props.put("kafka.kafka-connect-client.scrape-concurrency", "8");
    props.put("kafka.kafka-connect-client.max-retries", "10");
    props.put("kafka.kafka-connect-client.retry-base-delay-ms", "1000");
    props.put("kafka.kafka-connect-client.retry-max-delay-ms", "30000");

    ClustersProperties result = new Binder(new MapConfigurationPropertySource(props))
        .bind("kafka", ClustersProperties.class)
        .get();

    assertThat(result.getKafkaConnectClient().getScrapeConcurrency()).isEqualTo(8);
    assertThat(result.getKafkaConnectClient().getMaxRetries()).isEqualTo(10);
    assertThat(result.getKafkaConnectClient().getRetryBaseDelayMs()).isEqualTo(1000L);
    assertThat(result.getKafkaConnectClient().getRetryMaxDelayMs()).isEqualTo(30000L);
  }

  @Test
  void kafkaConnectConfigOverriddenViaEnvVarStyleProperties() {
    Map<String, String> props = new HashMap<>();
    props.put("kafka.kafkaconnectclient.scrapeconcurrency", "16");
    props.put("kafka.kafkaconnectclient.maxretries", "3");
    props.put("kafka.kafkaconnectclient.retrybasedelayms", "250");
    props.put("kafka.kafkaconnectclient.retrymaxdelayms", "5000");

    ClustersProperties result = new Binder(new MapConfigurationPropertySource(props))
        .bind("kafka", ClustersProperties.class)
        .get();

    assertThat(result.getKafkaConnectClient().getScrapeConcurrency()).isEqualTo(16);
    assertThat(result.getKafkaConnectClient().getMaxRetries()).isEqualTo(3);
    assertThat(result.getKafkaConnectClient().getRetryBaseDelayMs()).isEqualTo(250L);
    assertThat(result.getKafkaConnectClient().getRetryMaxDelayMs()).isEqualTo(5000L);
  }

  @Test
  void kafkaConnectConfigPartialOverrideKeepsDefaults() {
    Map<String, String> props = new HashMap<>();
    props.put("kafka.kafka-connect-client.scrape-concurrency", "12");

    ClustersProperties result = new Binder(new MapConfigurationPropertySource(props))
        .bind("kafka", ClustersProperties.class)
        .get();

    assertThat(result.getKafkaConnectClient().getScrapeConcurrency()).isEqualTo(12);
    // Others keep defaults
    assertThat(result.getKafkaConnectClient().getMaxRetries()).isEqualTo(5);
    assertThat(result.getKafkaConnectClient().getRetryBaseDelayMs()).isEqualTo(500L);
    assertThat(result.getKafkaConnectClient().getRetryMaxDelayMs()).isEqualTo(10000L);
  }

  @Test
  void kafkaConnectConfigRejectsInvalidValues() {
    ClustersProperties properties = new ClustersProperties();
    properties.getKafkaConnectClient().setScrapeConcurrency(0);
    properties.getKafkaConnectClient().setMaxRetries(-1);
    properties.getKafkaConnectClient().setRetryBaseDelayMs(0);
    properties.getKafkaConnectClient().setRetryMaxDelayMs(0);

    var violations = VALIDATOR.validate(properties);

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .contains(
            "kafkaConnectClient.scrapeConcurrency",
            "kafkaConnectClient.maxRetries",
            "kafkaConnectClient.retryBaseDelayMs",
            "kafkaConnectClient.retryMaxDelayMs"
        );
  }

  @Test
  void kafkaConnectConfigRejectsInvalidRetryRange() {
    ClustersProperties properties = new ClustersProperties();
    properties.getKafkaConnectClient().setRetryBaseDelayMs(2000);
    properties.getKafkaConnectClient().setRetryMaxDelayMs(1000);

    var violations = VALIDATOR.validate(properties);

    assertThat(violations)
        .extracting(v -> v.getMessage())
        .contains("retryMaxDelayMs must be greater than or equal to retryBaseDelayMs");
  }

}
