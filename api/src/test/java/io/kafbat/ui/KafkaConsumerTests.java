package io.kafbat.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

import io.kafbat.ui.model.BrokerConfigDTO;
import io.kafbat.ui.model.PartitionsIncreaseDTO;
import io.kafbat.ui.model.PartitionsIncreaseResponseDTO;
import io.kafbat.ui.model.TopicConfigDTO;
import io.kafbat.ui.model.TopicCreationDTO;
import io.kafbat.ui.model.TopicDetailsDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.producer.KafkaTestProducer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
class KafkaConsumerTests extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;


  @Test
  void shouldDeleteRecords() {
    var topicName = UUID.randomUUID().toString();
    webTestClient.post()
        .uri("/api/clusters/{clusterName}/topics", LOCAL)
        .bodyValue(new TopicCreationDTO()
            .name(topicName)
            .partitions(1)
            .replicationFactor(1)
            .configs(Map.of())
        )
        .exchange()
        .expectStatus()
        .isOk();

    try (KafkaTestProducer<String, String> producer = KafkaTestProducer.forKafka(kafka)) {
      Flux.fromStream(
          Stream.of("one", "two", "three", "four")
              .map(value -> Mono.fromFuture(producer.send(topicName, value)))
      ).blockLast();
    } catch (Throwable e) {
      log.error("Error on sending", e);
      throw new RuntimeException(e);
    }

    long count = Objects.requireNonNull(
        webTestClient.get()
            .uri("/api/clusters/{clusterName}/topics/{topicName}/messages/v2?mode=EARLIEST", LOCAL, topicName)
            .accept(TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(TopicMessageEventDTO.class)
            .returnResult()
            .getResponseBody()
        )
        .stream()
        .filter(e -> e.getType().equals(TopicMessageEventDTO.TypeEnum.MESSAGE))
        .count();

    assertThat(count).isEqualTo(4);

    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/messages", LOCAL, topicName)
        .exchange()
        .expectStatus()
        .isOk();

    count = Objects.requireNonNull(
        webTestClient.get()
            .uri("/api/clusters/{clusterName}/topics/{topicName}/messages/v2?mode=EARLIEST", LOCAL, topicName)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(TopicMessageEventDTO.class)
            .returnResult()
            .getResponseBody()
        )
        .stream()
        .filter(e -> e.getType().equals(TopicMessageEventDTO.TypeEnum.MESSAGE))
        .count();

    assertThat(count).isZero();
  }

  @Test
  void shouldIncreasePartitionsUpTo10() {
    var topicName = UUID.randomUUID().toString();
    webTestClient.post()
        .uri("/api/clusters/{clusterName}/topics", LOCAL)
        .bodyValue(new TopicCreationDTO()
            .name(topicName)
            .partitions(1)
            .replicationFactor(1)
            .configs(Map.of())
        )
        .exchange()
        .expectStatus()
        .isOk();

    PartitionsIncreaseResponseDTO response = webTestClient.patch()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/partitions",
            LOCAL,
            topicName)
        .bodyValue(new PartitionsIncreaseDTO()
            .totalPartitionsCount(10)
        )
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PartitionsIncreaseResponseDTO.class)
        .returnResult()
        .getResponseBody();

    Assertions.assertNotNull(response);
    Assertions.assertEquals(10, response.getTotalPartitionsCount());

    TopicDetailsDTO topicDetails = webTestClient.get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}",
            LOCAL,
            topicName)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TopicDetailsDTO.class)
        .returnResult()
        .getResponseBody();

    Assertions.assertNotNull(topicDetails);
    Assertions.assertEquals(10, topicDetails.getPartitionCount());
  }

  @Test
  void shouldReturn404ForNonExistingTopic() {
    var topicName = UUID.randomUUID().toString();

    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/messages", LOCAL, topicName)
        .exchange()
        .expectStatus()
        .isNotFound();

    webTestClient.get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/config", LOCAL, topicName)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldReturnConfigsForBroker() {
    List<BrokerConfigDTO> configs = webTestClient.get()
        .uri("/api/clusters/{clusterName}/brokers/{id}/configs",
            LOCAL,
            1)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(BrokerConfigDTO.class)
        .returnResult()
        .getResponseBody();

    Assertions.assertNotNull(configs);
    Assertions.assertFalse(configs.isEmpty());
    BrokerConfigDTO brokerConfigDto = configs.get(0);
    Assertions.assertNotNull(brokerConfigDto.getName());
    Assertions.assertNotNull(brokerConfigDto.getIsReadOnly());
    Assertions.assertNotNull(brokerConfigDto.getIsSensitive());
    Assertions.assertNotNull(brokerConfigDto.getSource());
    Assertions.assertNotNull(brokerConfigDto.getSynonyms());
  }

  @Test
  void shouldReturn404ForNonExistingBroker() {
    webTestClient.get()
        .uri("/api/clusters/{clusterName}/brokers/{id}/configs",
            LOCAL,
            0)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldRetrieveTopicConfig() {
    var topicName = UUID.randomUUID().toString();

    webTestClient.post()
            .uri("/api/clusters/{clusterName}/topics", LOCAL)
            .bodyValue(new TopicCreationDTO()
                    .name(topicName)
                    .partitions(1)
                    .replicationFactor(1)
                    .configs(Map.of())
            )
            .exchange()
            .expectStatus()
            .isOk();

    List<TopicConfigDTO> configs = webTestClient.get()
            .uri("/api/clusters/{clusterName}/topics/{topicName}/config", LOCAL, topicName)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(TopicConfigDTO.class)
            .returnResult()
            .getResponseBody();

    Assertions.assertNotNull(configs);
    Assertions.assertFalse(configs.isEmpty());
    TopicConfigDTO topicConfigDto = configs.get(0);
    Assertions.assertNotNull(topicConfigDto.getName());
    Assertions.assertNotNull(topicConfigDto.getIsReadOnly());
    Assertions.assertNotNull(topicConfigDto.getIsSensitive());
    Assertions.assertNotNull(topicConfigDto.getSource());
    Assertions.assertNotNull(topicConfigDto.getSynonyms());
  }
}
