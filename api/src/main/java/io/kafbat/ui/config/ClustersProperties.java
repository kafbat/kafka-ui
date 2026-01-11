package io.kafbat.ui.config;

import static io.kafbat.ui.model.MetricsScrapeProperties.JMX_METRICS_TYPE;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("kafka")
@Data
@Validated
public class ClustersProperties {

  List<@Valid Cluster> clusters = new ArrayList<>();

  String internalTopicPrefix;

  Integer adminClientTimeout;

  PollingProperties polling = new PollingProperties();

  MetricsStorage defaultMetricsStorage = new MetricsStorage();

  CacheProperties cache = new CacheProperties();
  ClusterFtsProperties fts = new ClusterFtsProperties();

  AdminClient adminClient = new AdminClient();

  Csv csv = new Csv();

  @Data
  public static class Csv {
    String lineDelimeter = "crlf";
    char quoteCharacter = '"';
    String quoteStrategy = "required";
    char fieldSeparator = ',';
  }

  @Data
  public static class AdminClient {
    Integer timeout;
    int describeConsumerGroupsPartitionSize = 50;
    int describeConsumerGroupsConcurrency = 4;
    int listConsumerGroupOffsetsPartitionSize = 50;
    int listConsumerGroupOffsetsConcurrency = 4;
    int getTopicsConfigPartitionSize = 200;
    int describeTopicsPartitionSize = 200;
  }

  @Data
  public static class Cluster {
    @NotBlank(message = "field name for for cluster could not be blank")
    String name;
    @NotBlank(message = "field bootstrapServers for for cluster could not be blank")
    String bootstrapServers;

    TruststoreConfig ssl;

    String schemaRegistry;
    SchemaRegistryAuth schemaRegistryAuth;
    KeystoreConfig schemaRegistrySsl;

    String ksqldbServer;
    KsqldbServerAuth ksqldbServerAuth;
    KeystoreConfig ksqldbServerSsl;

    List<@Valid ConnectCluster> kafkaConnect;

    List<@Valid SerdeConfig> serde;
    String defaultKeySerde;
    String defaultValueSerde;

    MetricsConfig metrics;
    Map<String, Object> properties;
    Map<String, Object> consumerProperties;
    Map<String, Object> producerProperties;
    boolean readOnly = false;

    Long pollingThrottleRate;

    List<@Valid Masking> masking;

    AuditProperties audit;
  }

  @Data
  public static class PollingProperties {
    Integer pollTimeoutMs;
    Integer maxPageSize;
    Integer defaultPageSize;
    Integer responseTimeoutMs;
  }

  @Data
  @ToString(exclude = {"password", "keystorePassword"})
  public static class MetricsConfig {
    String type;
    Integer port;
    Boolean ssl;
    String username;
    String password;
    String keystoreLocation;
    String keystorePassword;

    Boolean prometheusExpose;
    MetricsStorage store;
  }

  @Data
  public static class MetricsStorage {
    PrometheusStorage prometheus;
  }

  @Data
  @ToString(exclude = {"pushGatewayPassword"})
  public static class PrometheusStorage {
    String url;
    String pushGatewayUrl;
    String pushGatewayUsername;
    String pushGatewayPassword;
    String pushGatewayJobName;
    Boolean remoteWrite;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  @ToString(exclude = {"password", "keystorePassword"})
  public static class ConnectCluster {
    @NotBlank
    String name;
    @NotBlank
    String address;
    String username;
    String password;
    String keystoreLocation;
    String keystorePassword;
  }

  @Data
  @ToString(exclude = {"password"})
  public static class SchemaRegistryAuth {
    String username;
    String password;
  }

  @Data
  @ToString(exclude = {"truststorePassword"})
  public static class TruststoreConfig {
    String truststoreLocation;
    String truststorePassword;
    boolean verify = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString(exclude = {"keystorePassword"})
  public static class KeystoreConfig {
    String keystoreLocation;
    String keystorePassword;
  }

  @Data
  public static class SerdeConfig {
    @NotBlank
    String name;
    String className;
    String filePath;
    Map<String, Object> properties;
    String topicKeysPattern;
    String topicValuesPattern;
  }

  @Data
  @ToString(exclude = "password")
  public static class KsqldbServerAuth {
    String username;
    String password;
  }

  @Data
  public static class Masking {
    @NotNull
    Type type;
    List<String> fields;
    String fieldsNamePattern;
    List<String> maskingCharsReplacement; //used when type=MASK
    String replacement; //used when type=REPLACE
    String topicKeysPattern;
    String topicValuesPattern;

    public enum Type {
      REMOVE, MASK, REPLACE
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AuditProperties {
    String topic;
    Integer auditTopicsPartitions;
    Boolean topicAuditEnabled;
    Boolean consoleAuditEnabled;
    LogLevel level = LogLevel.ALTER_ONLY;
    Map<String, String> auditTopicProperties;
    Boolean requireAuditTopic;

    public enum LogLevel {
      ALL,
      ALTER_ONLY //default
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CacheProperties {
    boolean enabled = true;
    Duration connectClusterCacheExpiry = Duration.ofHours(24);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NgramProperties {
    int ngramMin = 1;
    int ngramMax = 4;
    boolean distanceScore = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClusterFtsProperties {
    boolean enabled = true;
    boolean defaultEnabled = false;
    NgramProperties schemas = new NgramProperties(1, 4, true);
    NgramProperties consumers = new NgramProperties(1, 4, true);
    NgramProperties connect = new NgramProperties(1, 4, true);
    NgramProperties acl = new NgramProperties(1, 4, true);

    public boolean use(Boolean request) {
      if (enabled) {
        if (Boolean.TRUE.equals(request)) {
          return true;
        } else {
          return request == null && defaultEnabled;
        }
      }
      return false;
    }
  }

  @PostConstruct
  public void validateAndSetDefaults() {
    if (clusters != null) {
      validateClusterNames();
      flattenClusterProperties();
      setMetricsDefaults();
    }
  }

  private void setMetricsDefaults() {
    for (Cluster cluster : clusters) {
      if (cluster.getMetrics() != null && !StringUtils.hasText(cluster.getMetrics().getType())) {
        cluster.getMetrics().setType(JMX_METRICS_TYPE);
      }
    }
  }

  private void flattenClusterProperties() {
    for (Cluster cluster : clusters) {
      cluster.setProperties(flattenClusterProperties(null, cluster.getProperties()));
      cluster.setConsumerProperties(flattenClusterProperties(null, cluster.getConsumerProperties()));
      cluster.setProducerProperties(flattenClusterProperties(null, cluster.getProducerProperties()));
    }
  }

  private Map<String, Object> flattenClusterProperties(@Nullable String prefix,
                                                       @Nullable Map<String, Object> propertiesMap) {
    Map<String, Object> flattened = new HashMap<>();
    if (propertiesMap != null) {
      propertiesMap.forEach((k, v) -> {
        String key = prefix == null ? k : prefix + "." + k;
        if (v instanceof Map<?, ?>) {
          flattened.putAll(flattenClusterProperties(key, (Map<String, Object>) v));
        } else {
          flattened.put(key, v);
        }
      });
    }
    return flattened;
  }

  private void validateClusterNames() {
    // if only one cluster provided it is ok not to set name
    if (clusters.size() == 1 && !StringUtils.hasText(clusters.getFirst().getName())) {
      clusters.getFirst().setName("Default");
      return;
    }

    Set<String> clusterNames = new HashSet<>();
    for (Cluster clusterProperties : clusters) {
      if (!StringUtils.hasText(clusterProperties.getName())) {
        throw new IllegalStateException(
            "Application config isn't valid. "
                + "Cluster names should be provided in case of multiple clusters present");
      }
      if (!clusterNames.add(clusterProperties.getName())) {
        throw new IllegalStateException(
            "Application config isn't valid. Two clusters can't have the same name");
      }
    }
  }
}
