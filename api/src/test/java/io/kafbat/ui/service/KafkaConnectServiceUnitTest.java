package io.kafbat.ui.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.api.KafkaConnectClientApi;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.mapper.KafkaConnectMapper;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.util.ReactiveFailover;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link KafkaConnectService#getConnectorsWithErrorsSuppress}.
 *
 * <p>Regression test for https://github.com/kafbat/kafka-ui/issues/1963: a Connect error used to
 * be swallowed via {@code onErrorComplete()} with nothing logged. These tests pin down that the
 * error still does not propagate to callers (existing behaviour they rely on) while also
 * reaching the logger instead of being discarded silently.
 */
class KafkaConnectServiceUnitTest {

  private static final String CLUSTER_NAME = "test-cluster";
  private static final String CONNECT_NAME = "kafka-connect";

  private final KafkaConnectService kafkaConnectService = new KafkaConnectService(
      mock(KafkaConnectMapper.class),
      mock(KafkaConfigSanitizer.class),
      mock(ClustersProperties.class),
      mock(StatisticsCache.class));

  private KafkaCluster clusterWith(KafkaConnectClientApi client) {
    return KafkaCluster.builder()
        .name(CLUSTER_NAME)
        .connectsClients(Map.of(CONNECT_NAME, ReactiveFailover.createNoop(client)))
        .build();
  }

  @Test
  void suppressesTheErrorInsteadOfPropagatingIt() {
    final KafkaConnectClientApi failingClient = mock(KafkaConnectClientApi.class);
    when(failingClient.getConnectors(isNull(), any())).thenReturn(Mono.error(
        new RuntimeException("500 from Connect: a connector's class can't be resolved")));

    // Existing callers (getAllConnectors, scrapeAllConnects, ...) flatMap straight over this
    // Mono and rely on it never erroring - only on completing empty when Connect is unreachable.
    StepVerifier.create(
            kafkaConnectService.getConnectorsWithErrorsSuppress(clusterWith(failingClient), CONNECT_NAME))
        .verifyComplete();
  }

  @Test
  void stillReturnsTheResultOnSuccess() {
    final Map<String, ExpandedConnector> connectors = Map.of();
    final KafkaConnectClientApi client = mock(KafkaConnectClientApi.class);
    when(client.getConnectors(isNull(), any())).thenReturn(Mono.just(connectors));

    StepVerifier.create(
            kafkaConnectService.getConnectorsWithErrorsSuppress(clusterWith(client), CONNECT_NAME))
        .expectNext(connectors)
        .verifyComplete();
  }
}
