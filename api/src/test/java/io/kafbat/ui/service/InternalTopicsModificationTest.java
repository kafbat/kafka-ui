package io.kafbat.ui.service;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.exception.InternalTopicModificationException;
import io.kafbat.ui.exception.TopicNotFoundException;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.PartitionsIncreaseDTO;
import io.kafbat.ui.model.ReplicationFactorChangeDTO;
import io.kafbat.ui.model.TopicUpdateDTO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.retry.Retry;

/**
 * Internal topics can't be modified via UI, so the API should reject such attempts as well.
 */
class InternalTopicsModificationTest extends AbstractIntegrationTest {

  @Autowired
  private TopicsService topicsService;

  @Autowired
  private MessagesService messagesService;

  @Autowired
  private WebTestClient webTestClient;

  private KafkaCluster cluster;
  private String internalTopic;

  @BeforeEach
  void init() {
    cluster = applicationContext
        .getBean(ClustersStorage.class)
        .getClusterByName(LOCAL)
        .orElseThrow();
    // default internal topic prefix is "_"
    internalTopic = "_" + InternalTopicsModificationTest.class.getSimpleName() + UUID.randomUUID();
    createTopicAndWaitUntilVisible(internalTopic);
  }

  /**
   * A freshly created topic is not immediately visible via describeTopics(), so every test would
   * otherwise race the broker's metadata propagation and see TopicNotFoundException.
   */
  private void createTopicAndWaitUntilVisible(String topicName) {
    createTopic(new NewTopic(topicName, 1, (short) 1));
    topicsService.getTopicDetails(cluster, topicName)
        .retryWhen(Retry.fixedDelay(40, Duration.ofMillis(250))
            .filter(TopicNotFoundException.class::isInstance))
        .block(Duration.ofSeconds(30));
  }

  @AfterEach
  void cleanup() {
    deleteTopic(internalTopic);
  }

  @Test
  void updateTopicIsRejected() {
    expectRejected(
        topicsService.updateTopic(
            cluster,
            internalTopic,
            Mono.just(new TopicUpdateDTO().configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, "12345")))));
  }

  @Test
  void deleteTopicIsRejected() {
    expectRejected(topicsService.deleteTopic(cluster, internalTopic));
  }

  @Test
  void recreateTopicIsRejected() {
    expectRejected(topicsService.recreateTopic(cluster, internalTopic));
  }

  @Test
  void cloneTopicIsRejected() {
    expectRejected(topicsService.cloneTopic(cluster, internalTopic, internalTopic + "-clone"));
  }

  @Test
  void increaseTopicPartitionsIsRejected() {
    expectRejected(
        topicsService.increaseTopicPartitions(
            cluster, internalTopic, new PartitionsIncreaseDTO().totalPartitionsCount(5)));
  }

  @Test
  void changeReplicationFactorIsRejected() {
    expectRejected(
        topicsService.changeReplicationFactor(
            cluster, internalTopic, new ReplicationFactorChangeDTO().totalReplicationFactor(2)));
  }

  @Test
  void deleteTopicMessagesIsRejected() {
    expectRejected(messagesService.deleteTopicMessages(cluster, internalTopic, List.of()));
  }

  @Test
  void deleteTopicEndpointRespondsWithBadRequest() {
    webTestClient.delete()
        .uri("/api/clusters/{clusterName}/topics/{topicName}", LOCAL, internalTopic)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void updateTopicEndpointRespondsWithBadRequest() {
    webTestClient.patch()
        .uri("/api/clusters/{clusterName}/topics/{topicName}", LOCAL, internalTopic)
        .bodyValue(new TopicUpdateDTO().configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, "12345")))
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void nonInternalTopicIsStillModifiable() {
    String topic = InternalTopicsModificationTest.class.getSimpleName() + UUID.randomUUID();
    createTopicAndWaitUntilVisible(topic);
    try {
      StepVerifier.create(
              topicsService.updateTopic(
                  cluster,
                  topic,
                  Mono.just(
                      new TopicUpdateDTO().configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, "12345")))))
          .expectNextCount(1)
          .verifyComplete();
    } finally {
      deleteTopic(topic);
    }
  }

  private void expectRejected(Publisher<?> publisher) {
    StepVerifier.create(publisher)
        .expectError(InternalTopicModificationException.class)
        .verify();
  }
}
