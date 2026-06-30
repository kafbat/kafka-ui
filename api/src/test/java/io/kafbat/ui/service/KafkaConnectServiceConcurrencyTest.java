package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.model.ClusterInfo;
import io.kafbat.ui.connect.model.Connector;
import io.kafbat.ui.connect.model.ConnectorStatus;
import io.kafbat.ui.connect.model.ConnectorStatusConnector;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.mapper.KafkaConnectMapperImpl;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.kafbat.ui.util.ReactiveFailover;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class KafkaConnectServiceConcurrencyTest {

  private static final int CONCURRENCY = 2;
  private static final int CONNECTOR_COUNT = 10;

  private KafkaConnectService service;
  private final AtomicInteger concurrentCalls = new AtomicInteger(0);
  private final AtomicInteger maxConcurrentCalls = new AtomicInteger(0);

  @BeforeEach
  void setUp() {
    concurrentCalls.set(0);
    maxConcurrentCalls.set(0);

    var clustersProperties = new ClustersProperties();
    var kafkaConnectConfig = new ClustersProperties.KafkaConnect();
    kafkaConnectConfig.setScrapeConcurrency(CONCURRENCY);
    clustersProperties.setKafkaConnectClient(kafkaConnectConfig);

    var statisticsCache = mock(StatisticsCache.class);
    var statistics = Statistics.builder()
        .connectStates(Map.of())
        .clusterState(mock(ScrapedClusterState.class))
        .build();
    when(statisticsCache.get(any())).thenReturn(statistics);

    var mapper = new KafkaConnectMapperImpl();
    KafkaConfigSanitizer sanitizer = new KafkaConfigSanitizer(true, List.of());

    service = new KafkaConnectService(mapper, sanitizer, clustersProperties, statisticsCache);
  }

  @SuppressWarnings("unchecked")
  private ReactiveFailover<io.kafbat.ui.connect.api.KafkaConnectClientApi> mockConnectApi() {
    var api = mock(io.kafbat.ui.connect.api.KafkaConnectClientApi.class);

    // getClusterInfo
    when(api.getClusterInfo()).thenReturn(Mono.just(new ClusterInfo().version("3.0")));

    // getConnectors - returns N connectors
    Map<String, ExpandedConnector> connectors = IntStream.range(0, CONNECTOR_COUNT)
        .boxed()
        .collect(Collectors.toMap(
            i -> "connector-" + i,
            i -> new ExpandedConnector()
                .info(new Connector()
                    .name("connector-" + i)
                    .type(Connector.TypeEnum.SINK)
                    .config(Map.of("name", "connector-" + i, "connector.class", "TestClass"))
                    .tasks(List.of()))
                .status(new ConnectorStatus()
                    .name("connector-" + i)
                    .tasks(List.of())
                    .connector(new ConnectorStatusConnector()
                        .state(ConnectorStatusConnector.StateEnum.RUNNING)))
        ));
    when(api.getConnectors(any(), any())).thenReturn(Mono.just(connectors));

    // getConnectorTopics - track concurrency at subscription time
    when(api.getConnectorTopics(anyString())).thenAnswer(invocation -> {
      String name = invocation.getArgument(0);
      return Mono.defer(() -> {
        int current = concurrentCalls.incrementAndGet();
        maxConcurrentCalls.updateAndGet(max -> Math.max(max, current));
        return Mono.just(Map.of(name, new ConnectorTopics().topics(List.of("topic1"))))
            .delayElement(Duration.ofMillis(100))
            .doOnTerminate(() -> concurrentCalls.decrementAndGet());
      });
    });

    var failover = mock(ReactiveFailover.class);
    when(failover.mono(any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      var fn = (java.util.function.Function<io.kafbat.ui.connect.api.KafkaConnectClientApi, Mono<?>>) invocation
          .getArgument(0);
      // Defer so fn.apply() runs at subscription time, not invocation time
      return Mono.defer(() -> fn.apply(api));
    });
    when(failover.flux(any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      var fn = (java.util.function.Function<io.kafbat.ui.connect.api.KafkaConnectClientApi, reactor.core.publisher.Flux<?>>) invocation
          .getArgument(0);
      return reactor.core.publisher.Flux.defer(() -> fn.apply(api));
    });
    return failover;
  }

  private KafkaCluster buildCluster(
      ReactiveFailover<io.kafbat.ui.connect.api.KafkaConnectClientApi> connectApi) {
    var connectCluster = ClustersProperties.ConnectCluster.builder()
        .name("test-connect")
        .address("http://localhost:8083")
        .consumerNamePattern("connect-%s")
        .build();
    var clusterProps = new ClustersProperties.Cluster();
    clusterProps.setName("test-cluster");
    clusterProps.setBootstrapServers("localhost:9092");
    clusterProps.setKafkaConnect(List.of(connectCluster));

    return KafkaCluster.builder()
        .name("test-cluster")
        .bootstrapServers("localhost:9092")
        .originalProperties(clusterProps)
        .connectsClients(Map.of("test-connect", connectApi))
        .connectsConfigs(Map.of("test-connect", connectCluster))
        .build();
  }

  @Test
  void scrapeAllConnectsLimitsConcurrency() {
    var connectApi = mockConnectApi();
    var cluster = buildCluster(connectApi);

    StepVerifier.create(service.scrapeAllConnects(cluster).collectList())
        .assertNext(states -> {
          assertThat(states).hasSize(1);
          KafkaConnectState state = states.getFirst();
          assertThat(state.getConnectors()).hasSize(CONNECTOR_COUNT);
        })
        .verifyComplete();

    // maxConcurrentCalls should not exceed CONCURRENCY
    assertThat(maxConcurrentCalls.get())
        .as("Max concurrent getConnectorTopics calls should not exceed configured concurrency (%d)", CONCURRENCY)
        .isLessThanOrEqualTo(CONCURRENCY);
  }

  @Test
  void scrapeAllConnectsWithHigherConcurrency() {
    // Reconfigure with higher concurrency
    concurrentCalls.set(0);
    maxConcurrentCalls.set(0);

    var clustersProperties = new ClustersProperties();
    var kafkaConnectConfig = new ClustersProperties.KafkaConnect();
    kafkaConnectConfig.setScrapeConcurrency(8);
    clustersProperties.setKafkaConnectClient(kafkaConnectConfig);

    var statisticsCache = mock(StatisticsCache.class);
    var statistics = Statistics.builder()
        .connectStates(Map.of())
        .clusterState(mock(ScrapedClusterState.class))
        .build();
    when(statisticsCache.get(any())).thenReturn(statistics);

    service = new KafkaConnectService(
        new KafkaConnectMapperImpl(),
        new KafkaConfigSanitizer(true, List.of()),
        clustersProperties,
        statisticsCache
    );

    var connectApi = mockConnectApi();
    var cluster = buildCluster(connectApi);

    StepVerifier.create(service.scrapeAllConnects(cluster).collectList())
        .assertNext(states -> assertThat(states).hasSize(1))
        .verifyComplete();

    // With concurrency=8 and 10 connectors, max should be at most 8
    assertThat(maxConcurrentCalls.get())
        .as("Max concurrent calls should not exceed configured concurrency (8)")
        .isLessThanOrEqualTo(8);

    // But should be higher than the restrictive limit of 2
    assertThat(maxConcurrentCalls.get())
        .as("Max concurrent calls should be higher than 2 when concurrency=8")
        .isGreaterThan(CONCURRENCY);
  }
}
