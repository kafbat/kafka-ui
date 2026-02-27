package io.kafbat.ui.service;

import static io.kafbat.ui.util.KafkaServicesValidation.validateClusterConnection;
import static io.kafbat.ui.util.KafkaServicesValidation.validateKsql;
import static io.kafbat.ui.util.KafkaServicesValidation.validatePrometheusStore;
import static io.kafbat.ui.util.KafkaServicesValidation.validateSchemaRegistry;
import static io.kafbat.ui.util.KafkaServicesValidation.validateTruststore;

import io.kafbat.ui.client.RetryingKafkaConnectClient;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.config.WebclientProperties;
import io.kafbat.ui.connect.api.KafkaConnectClientApi;
import io.kafbat.ui.emitter.PollingSettings;
import io.kafbat.ui.model.ApplicationPropertyValidationDTO;
import io.kafbat.ui.model.ClusterConfigValidationDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.prometheus.api.PrometheusClientApi;
import io.kafbat.ui.service.ksql.KsqlApiClient;
import io.kafbat.ui.service.masking.DataMasking;
import io.kafbat.ui.service.metrics.scrape.MetricsScraper;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsRetriever;
import io.kafbat.ui.sr.ApiClient;
import io.kafbat.ui.sr.api.KafkaSrClientApi;
import io.kafbat.ui.util.KafkaServicesValidation;
import io.kafbat.ui.util.ReactiveFailover;
import io.kafbat.ui.util.WebClientConfigurator;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@Slf4j
public class KafkaClusterFactory {

  private static final DataSize DEFAULT_WEBCLIENT_BUFFER = DataSize.parse("20MB");
  private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(20);

  // Confluent Schema Registry API content types (used by WarpStream and other compatible implementations)
  private static final MediaType SR_V1_JSON = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json");
  private static final MediaType SR_JSON = MediaType.parseMediaType("application/vnd.schemaregistry+json");

  private final DataSize webClientMaxBuffSize;
  private final Duration responseTimeout;
  private final JmxMetricsRetriever jmxMetricsRetriever;

  public KafkaClusterFactory(WebclientProperties webclientProperties,
                             JmxMetricsRetriever jmxMetricsRetriever) {
    this.webClientMaxBuffSize = Optional.ofNullable(webclientProperties.getMaxInMemoryBufferSize())
        .map(DataSize::parse)
        .orElse(DEFAULT_WEBCLIENT_BUFFER);
    this.responseTimeout = Optional.ofNullable(webclientProperties.getResponseTimeoutMs())
        .map(Duration::ofMillis)
        .orElse(DEFAULT_RESPONSE_TIMEOUT);
    this.jmxMetricsRetriever = jmxMetricsRetriever;
  }

  public KafkaCluster create(ClustersProperties properties,
                             ClustersProperties.Cluster clusterProperties) {
    KafkaCluster.KafkaClusterBuilder builder = KafkaCluster.builder();

    builder.name(clusterProperties.getName());
    builder.bootstrapServers(clusterProperties.getBootstrapServers());
    builder.properties(convertProperties(clusterProperties.getProperties()));
    builder.consumerProperties(convertProperties(clusterProperties.getConsumerProperties()));
    builder.producerProperties(convertProperties(clusterProperties.getProducerProperties()));
    builder.readOnly(clusterProperties.isReadOnly());
    builder.exposeMetricsViaPrometheusEndpoint(exposeMetricsViaPrometheusEndpoint(clusterProperties));
    builder.masking(DataMasking.create(clusterProperties.getMasking()));
    builder.pollingSettings(PollingSettings.create(clusterProperties, properties));
    builder.metricsScrapping(MetricsScraper.create(clusterProperties, jmxMetricsRetriever));

    if (schemaRegistryConfigured(clusterProperties)) {
      builder.schemaRegistryClient(schemaRegistryClient(clusterProperties));
    }
    if (connectClientsConfigured(clusterProperties)) {
      builder.connectsClients(connectClients(clusterProperties));
    }
    if (ksqlConfigured(clusterProperties)) {
      builder.ksqlClient(ksqlClient(clusterProperties));
    }
    if (prometheusStorageConfigured(properties.getDefaultMetricsStorage())) {
      builder.prometheusStorageClient(
          prometheusStorageClient(properties.getDefaultMetricsStorage(), clusterProperties.getSsl())
      );
    }
    if (prometheusStorageConfigured(clusterProperties)) {
      builder.prometheusStorageClient(prometheusStorageClient(
          clusterProperties.getMetrics().getStore(),
          clusterProperties.getSsl())
      );
    }

    builder.originalProperties(clusterProperties);
    return builder.build();
  }

  public Mono<ClusterConfigValidationDTO> validate(ClustersProperties.Cluster clusterProperties) {
    if (clusterProperties.getSsl() != null) {
      Optional<String> errMsg = validateTruststore(clusterProperties.getSsl());
      if (errMsg.isPresent()) {
        return Mono.just(new ClusterConfigValidationDTO()
            .kafka(new ApplicationPropertyValidationDTO()
                .error(true)
                .errorMessage("Truststore not valid: " + errMsg.get())));
      }
    }

    return Mono.zip(
        validateClusterConnection(
            clusterProperties.getBootstrapServers(),
            convertProperties(clusterProperties.getProperties()),
            clusterProperties.getSsl()
        ),
        schemaRegistryConfigured(clusterProperties)
            ? validateSchemaRegistry(() -> schemaRegistryClient(clusterProperties)).map(Optional::of)
            : Mono.<Optional<ApplicationPropertyValidationDTO>>just(Optional.empty()),

        ksqlConfigured(clusterProperties)
            ? validateKsql(() -> ksqlClient(clusterProperties)).map(Optional::of)
            : Mono.<Optional<ApplicationPropertyValidationDTO>>just(Optional.empty()),

        connectClientsConfigured(clusterProperties)
            ? Flux.fromIterable(clusterProperties.getKafkaConnect())
            .flatMap(c ->
                KafkaServicesValidation.validateConnect(() -> connectClient(clusterProperties, c))
                    .map(r -> Tuples.of(c.getName(), r)))
            .collectMap(Tuple2::getT1, Tuple2::getT2)
            .map(Optional::of)
            : Mono.<Optional<Map<String, ApplicationPropertyValidationDTO>>>just(Optional.empty()),

        prometheusStorageConfigured(clusterProperties)
            ? validatePrometheusStore(() -> prometheusStorageClient(
                clusterProperties.getMetrics().getStore(), clusterProperties.getSsl())).map(Optional::of)
            : Mono.<Optional<ApplicationPropertyValidationDTO>>just(Optional.empty())
    ).map(tuple -> {
      var validation = new ClusterConfigValidationDTO();
      validation.kafka(tuple.getT1());
      tuple.getT2().ifPresent(validation::schemaRegistry);
      tuple.getT3().ifPresent(validation::ksqldb);
      tuple.getT4().ifPresent(validation::kafkaConnects);
      tuple.getT5().ifPresent(validation::prometheusStorage);
      return validation;
    });
  }

  private boolean exposeMetricsViaPrometheusEndpoint(ClustersProperties.Cluster clusterProperties) {
    return Optional.ofNullable(clusterProperties.getMetrics())
        .map(m -> m.getPrometheusExpose() == null || m.getPrometheusExpose())
        .orElse(true);
  }

  private Properties convertProperties(Map<String, Object> propertiesMap) {
    Properties properties = new Properties();
    if (propertiesMap != null) {
      properties.putAll(propertiesMap);
    }
    return properties;
  }

  private ReactiveFailover<PrometheusClientApi> prometheusStorageClient(
      ClustersProperties.MetricsStorage storage, ClustersProperties.TruststoreConfig ssl) {
    WebClient webClient = new WebClientConfigurator()
        .configureSsl(ssl, null)
        .configureBufferSize(webClientMaxBuffSize)
        .build();
    return ReactiveFailover.create(
        parseUrlList(storage.getPrometheus().getUrl()),
        url -> new PrometheusClientApi(new io.kafbat.ui.prometheus.ApiClient(webClient).setBasePath(url)),
        ReactiveFailover.CONNECTION_REFUSED_EXCEPTION_FILTER,
        "No live Prometheus instances available",
        ReactiveFailover.DEFAULT_RETRY_GRACE_PERIOD_MS
    );
  }

  private boolean prometheusStorageConfigured(ClustersProperties.Cluster cluster) {
    return Optional.ofNullable(cluster.getMetrics())
        .flatMap(m -> Optional.ofNullable(m.getStore()))
        .map(this::prometheusStorageConfigured)
        .orElse(false);
  }

  private boolean prometheusStorageConfigured(ClustersProperties.MetricsStorage storage) {
    return Optional.ofNullable(storage)
        .flatMap(s -> Optional.ofNullable(s.getPrometheus()))
        .map(p -> StringUtils.hasText(p.getUrl()))
        .orElse(false);
  }

  private boolean connectClientsConfigured(ClustersProperties.Cluster clusterProperties) {
    return clusterProperties.getKafkaConnect() != null;
  }

  private Map<String, ReactiveFailover<KafkaConnectClientApi>> connectClients(
      ClustersProperties.Cluster clusterProperties) {
    Map<String, ReactiveFailover<KafkaConnectClientApi>> connects = new HashMap<>();
    clusterProperties.getKafkaConnect().forEach(c -> connects.put(c.getName(), connectClient(clusterProperties, c)));
    return connects;
  }

  private ReactiveFailover<KafkaConnectClientApi> connectClient(ClustersProperties.Cluster cluster,
                                                                ClustersProperties.ConnectCluster connectCluster) {
    return ReactiveFailover.create(
        parseUrlList(connectCluster.getAddress()),
        url -> new RetryingKafkaConnectClient(
            connectCluster.toBuilder().address(url).build(),
            cluster.getSsl(),
            webClientMaxBuffSize,
            responseTimeout
        ),
        ReactiveFailover.CONNECTION_REFUSED_EXCEPTION_FILTER,
        "No alive connect instances available",
        ReactiveFailover.DEFAULT_RETRY_GRACE_PERIOD_MS
    );
  }

  private boolean schemaRegistryConfigured(ClustersProperties.Cluster clusterProperties) {
    return clusterProperties.getSchemaRegistry() != null;
  }

  private ReactiveFailover<KafkaSrClientApi> schemaRegistryClient(ClustersProperties.Cluster clusterProperties) {
    var auth = Optional.ofNullable(clusterProperties.getSchemaRegistryAuth())
        .orElse(new ClustersProperties.SchemaRegistryAuth());
    WebClient webClient = new WebClientConfigurator()
        .configureSsl(clusterProperties.getSsl(), clusterProperties.getSchemaRegistrySsl())
        .configureBasicAuth(auth.getUsername(), auth.getPassword())
        .configureDefaultHeaders(clusterProperties.getSchemaRegistryHeaders())
        .configureBufferSize(webClientMaxBuffSize)
        .configureAdditionalDecoderMediaTypes(SR_V1_JSON, SR_JSON)
        .build();
    return ReactiveFailover.create(
        parseUrlList(clusterProperties.getSchemaRegistry()),
        url -> new KafkaSrClientApi(new ApiClient(webClient, null, null).setBasePath(url)),
        ReactiveFailover.CONNECTION_REFUSED_EXCEPTION_FILTER,
        "No live schemaRegistry instances available",
        ReactiveFailover.DEFAULT_RETRY_GRACE_PERIOD_MS
    );
  }

  private boolean ksqlConfigured(ClustersProperties.Cluster clusterProperties) {
    return clusterProperties.getKsqldbServer() != null;
  }

  private ReactiveFailover<KsqlApiClient> ksqlClient(ClustersProperties.Cluster clusterProperties) {
    return ReactiveFailover.create(
        parseUrlList(clusterProperties.getKsqldbServer()),
        url -> new KsqlApiClient(
            url,
            clusterProperties.getKsqldbServerAuth(),
            clusterProperties.getSsl(),
            clusterProperties.getKsqldbServerSsl(),
            webClientMaxBuffSize
        ),
        ReactiveFailover.CONNECTION_REFUSED_EXCEPTION_FILTER,
        "No live ksqldb instances available",
        ReactiveFailover.DEFAULT_RETRY_GRACE_PERIOD_MS
    );
  }

  private List<String> parseUrlList(String url) {
    return Stream.of(url.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
  }
}
