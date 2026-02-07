package io.kafbat.ui.model;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.api.KafkaConnectClientApi;
import io.kafbat.ui.emitter.PollingSettings;
import io.kafbat.ui.prometheus.api.PrometheusClientApi;
import io.kafbat.ui.service.ksql.KsqlApiClient;
import io.kafbat.ui.service.masking.DataMasking;
import io.kafbat.ui.service.metrics.scrape.MetricsScraper;
import io.kafbat.ui.sr.api.KafkaSrClientApi;
import io.kafbat.ui.util.ReactiveFailover;
import java.util.Map;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaCluster {
  private final ClustersProperties.Cluster originalProperties;

  private final String name;
  private final String version;
  private final String bootstrapServers;
  private final Properties properties;
  private final Properties consumerProperties;
  private final Properties producerProperties;
  private final boolean readOnly;
  private final boolean exposeMetricsViaPrometheusEndpoint;
  private final DataMasking masking;
  private final PollingSettings pollingSettings;
  private final MetricsScraper metricsScrapping;
  private final ReactiveFailover<KafkaSrClientApi> schemaRegistryClient;
  private final String schemaRegistryTopicSubjectSuffix;
  private final Map<String, ReactiveFailover<KafkaConnectClientApi>> connectsClients;
  private final ReactiveFailover<KsqlApiClient> ksqlClient;
  private final ReactiveFailover<PrometheusClientApi> prometheusStorageClient;
}
