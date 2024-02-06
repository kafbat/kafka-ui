package io.kafbat.ui.service;

import static io.kafbat.ui.service.MessagesService.execSmartFilterTest;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.exception.TopicNotFoundException;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.SeekDirectionDTO;
import io.kafbat.ui.model.SeekTypeDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionResultDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.producer.KafkaTestProducer;
import io.kafbat.ui.serdes.builtin.StringSerde;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class MessagesServiceTest extends AbstractIntegrationTest {

  private static final String MASKED_TOPICS_PREFIX = "masking-test-";
  private static final String NON_EXISTING_TOPIC = UUID.randomUUID().toString();

  @Autowired
  MessagesService messagesService;

  KafkaCluster cluster;

  @BeforeEach
  void init() {
    cluster = applicationContext
        .getBean(ClustersStorage.class)
        .getClusterByName(LOCAL)
        .get();
  }

  @Test
  void deleteTopicMessagesReturnsExceptionWhenTopicNotFound() {
    StepVerifier.create(messagesService.deleteTopicMessages(cluster, NON_EXISTING_TOPIC, List.of()))
        .expectError(TopicNotFoundException.class)
        .verify();
  }

  @Test
  void sendMessageReturnsExceptionWhenTopicNotFound() {
    StepVerifier.create(messagesService.sendMessage(cluster, NON_EXISTING_TOPIC, new CreateTopicMessageDTO()))
        .expectError(TopicNotFoundException.class)
        .verify();
  }

  @Test
  void loadMessagesReturnsExceptionWhenTopicNotFound() {
    StepVerifier.create(messagesService
            .loadMessages(cluster, NON_EXISTING_TOPIC, null, null, null, 1, null, "String", "String"))
        .expectError(TopicNotFoundException.class)
        .verify();
  }

  @Test
  void maskingAppliedOnConfiguredClusters() throws Exception {
    String testTopic = MASKED_TOPICS_PREFIX + UUID.randomUUID();
    try (var producer = KafkaTestProducer.forKafka(kafka)) {
      createTopic(new NewTopic(testTopic, 1, (short) 1));
      producer.send(testTopic, "message1");
      producer.send(testTopic, "message2").get();

      Flux<TopicMessageDTO> msgsFlux = messagesService.loadMessages(
              cluster,
              testTopic,
              new ConsumerPosition(SeekTypeDTO.BEGINNING, testTopic, null),
              null,
              null,
              100,
              SeekDirectionDTO.FORWARD,
              StringSerde.name(),
              StringSerde.name()
          ).filter(evt -> evt.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE)
          .map(TopicMessageEventDTO::getMessage);

      // both messages should be masked
      StepVerifier.create(msgsFlux)
          .expectNextMatches(msg -> msg.getContent().equals("***"))
          .expectNextMatches(msg -> msg.getContent().equals("***"))
          .verifyComplete();
    } finally {
      deleteTopic(testTopic);
    }
  }

  @Test
  void execSmartFilterTestReturnsExecutionResult() {
    var params = new SmartFilterTestExecutionDTO()
        .filterCode("has(record.key) && has(record.value) && record.headers.size() != 0 "
            + "&& has(record.timestampMs) && has(record.offset)")
        .key("1234")
        .value("{ \"some\" : \"value\" } ")
        .headers(Map.of("h1", "hv1"))
        .offset(12345L)
        .timestampMs(System.currentTimeMillis())
        .partition(1);

    var actual = execSmartFilterTest(params);
    assertThat(actual.getError()).isNull();
    assertThat(actual.getResult()).isTrue();

    params.setFilterCode("false");
    actual = execSmartFilterTest(params);
    assertThat(actual.getError()).isNull();
    assertThat(actual.getResult()).isFalse();
  }

  @Test
  void execSmartFilterTestCompilesToNonBooleanExpression() {
    var result = execSmartFilterTest(
        new SmartFilterTestExecutionDTO()
            .filterCode("1/0")
    );
    assertThat(result.getResult()).isNull();
    assertThat(result.getError()).containsIgnoringCase("Compilation error");
  }

  @Test
  void execSmartFilterTestReturnsErrorOnFilterApplyError() {
    var result = execSmartFilterTest(
        new SmartFilterTestExecutionDTO()
            .filterCode("1/0 == 1")
    );
    assertThat(result.getResult()).isNull();
    assertThat(result.getError()).containsIgnoringCase("execution error");
  }

  @Test
  void execSmartFilterTestReturnsErrorOnFilterCompilationError() {
    var result = execSmartFilterTest(
        new SmartFilterTestExecutionDTO()
            .filterCode("this is invalid CEL syntax = 1")
    );
    assertThat(result.getResult()).isNull();
    assertThat(result.getError()).containsIgnoringCase("Compilation error");
  }

}
