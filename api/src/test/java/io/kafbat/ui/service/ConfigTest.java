package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.BrokerConfigDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.ServerStatusDTO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.IsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

class ConfigTest extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @BeforeEach
  void waitUntilStatsInitialized() {
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollInSameThread()
        .until(() -> {
          var stats = applicationContext.getBean(StatisticsCache.class)
              .get(KafkaCluster.builder().name(LOCAL).build());
          return stats.getStatus() == ServerStatusDTO.ONLINE;
        });
  }

  @Test
  void testAlterConfig() {
    String name = "background.threads";

    Optional<BrokerConfigDTO> bc = getConfig(name);
    assertThat(bc.isPresent()).isTrue();
    assertThat(bc.get().getValue()).isEqualTo("10");

    final String newValue = "5";

    webTestClient.put()
        .uri("/api/clusters/{clusterName}/brokers/{id}/configs/{name}", LOCAL, 1, name)
        .bodyValue(Map.of(
            "name", name,
            "value", newValue
            )
        )
        .exchange()
        .expectStatus().isOk();

    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollInSameThread()
        .untilAsserted(() -> {
          Optional<BrokerConfigDTO> bcc = getConfig(name);
          assertThat(bcc.isPresent()).isTrue();
          assertThat(bcc.get().getValue()).isEqualTo(newValue);
        });
  }

  @Test
  void testAlterReadonlyConfig() {
    String name = "log.dirs";

    webTestClient.put()
        .uri("/api/clusters/{clusterName}/brokers/{id}/configs/{name}", LOCAL, 1, name)
        .bodyValue(Map.of(
            "name", name,
            "value", "/var/lib/kafka2"
            )
        )
        .exchange()
        .expectStatus().isBadRequest();
  }

  @Test
  void testKafkaClientCustomProperties() {
    KafkaCluster cluster = applicationContext.getBean(ClustersStorage.class).getClusterByName(LOCAL).orElseThrow();

    Properties consumerProps = cluster.getConsumerProperties();

    assertEquals("60000", consumerProps.getProperty(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG));
    assertEquals(IsolationLevel.READ_COMMITTED.toString(),
        consumerProps.getProperty(ConsumerConfig.ISOLATION_LEVEL_CONFIG));

    Properties producerProps = cluster.getProducerProperties();

    assertEquals("45000", producerProps.getProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
    assertEquals("80000", producerProps.getProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG));
  }

  private Optional<BrokerConfigDTO> getConfig(String name) {
    List<BrokerConfigDTO> configs = webTestClient.get()
        .uri("/api/clusters/{clusterName}/brokers/{id}/configs", LOCAL, 1)
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<List<BrokerConfigDTO>>() {
        })
        .returnResult()
        .getResponseBody();

    assertThat(configs).isNotNull();

    return configs.stream()
        .filter(c -> c.getName().equals(name))
        .findAny();
  }
}
