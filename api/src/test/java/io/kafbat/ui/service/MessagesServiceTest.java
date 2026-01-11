package io.kafbat.ui.service;

import static io.kafbat.ui.service.MessagesService.execSmartFilterTest;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.exception.TopicNotFoundException;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.SmartFilterTestExecutionDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.producer.KafkaTestProducer;
import io.kafbat.ui.serdes.builtin.ProtobufFileSerde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class MessagesServiceTest extends AbstractIntegrationTest {

  private static final String MASKED_TOPICS_PREFIX = "masking-test-";
  private static final String NON_EXISTING_TOPIC = UUID.randomUUID().toString();

  @Autowired
  MessagesService messagesService;

  KafkaCluster cluster;

  Set<String> createdTopics = new HashSet<>();

  @BeforeEach
  void init() {
    cluster = applicationContext
        .getBean(ClustersStorage.class)
        .getClusterByName(LOCAL)
        .orElseThrow();
  }

  @AfterEach
  void deleteCreatedTopics() {
    createdTopics.forEach(MessagesServiceTest::deleteTopic);
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
            .loadMessages(cluster, NON_EXISTING_TOPIC,
                new ConsumerPosition(PollingModeDTO.TAILING, NON_EXISTING_TOPIC, List.of(), null, null, null),
                null, null, 1, "String", "String"))
        .expectError(TopicNotFoundException.class)
        .verify();
  }

  @Test
  void maskingAppliedOnConfiguredClusters() throws Exception {
    String testTopic = MASKED_TOPICS_PREFIX + UUID.randomUUID();
    createTopicWithCleanup(new NewTopic(testTopic, 1, (short) 1));

    try (var producer = KafkaTestProducer.forKafka(kafka)) {
      producer.send(testTopic, "message1");
      producer.send(testTopic, "message2").get();
    }

    Flux<TopicMessageDTO> msgsFlux = messagesService.loadMessages(
            cluster,
            testTopic,
            new ConsumerPosition(PollingModeDTO.EARLIEST, testTopic, List.of(), null, null, null),
            null,
            null,
            100,
            StringSerde.NAME,
            StringSerde.NAME
        ).filter(evt -> evt.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE)
        .map(TopicMessageEventDTO::getMessage);

    // both messages should be masked
    StepVerifier.create(msgsFlux)
        .expectNextMatches(msg -> msg.getValue().equals("***"))
        .expectNextMatches(msg -> msg.getValue().equals("***"))
        .verifyComplete();
  }

  @ParameterizedTest
  @CsvSource({"EARLIEST", "LATEST"})
  void cursorIsRegisteredAfterPollingIsDoneAndCanBeUsedForNextPagePolling(PollingModeDTO mode) {
    String testTopic = MessagesServiceTest.class.getSimpleName() + UUID.randomUUID();
    createTopicWithCleanup(new NewTopic(testTopic, 5, (short) 1));

    int msgsToGenerate = 100;
    int pageSize = (msgsToGenerate / 2) + 1;

    try (var producer = KafkaTestProducer.forKafka(kafka)) {
      for (int i = 0; i < msgsToGenerate; i++) {
        producer.send(testTopic, "message_" + i);
      }
    }

    var cursorIdCatcher = new AtomicReference<String>();
    Flux<String> msgsFlux = messagesService.loadMessages(
            cluster, testTopic,
            new ConsumerPosition(mode, testTopic, List.of(), null, null, null),
            null, null, pageSize, StringSerde.NAME, StringSerde.NAME)
        .doOnNext(evt -> {
          if (evt.getType() == TopicMessageEventDTO.TypeEnum.DONE) {
            assertThat(evt.getCursor()).isNotNull();
            cursorIdCatcher.set(evt.getCursor().getId());
          }
        })
        .filter(evt -> evt.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE)
        .map(evt -> evt.getMessage().getValue());

    StepVerifier.create(msgsFlux)
        .expectNextCount(pageSize)
        .verifyComplete();

    assertThat(cursorIdCatcher.get()).isNotNull();

    Flux<String> remainingMsgs = messagesService.loadMessages(cluster, testTopic, cursorIdCatcher.get())
        .doOnNext(evt -> {
          if (evt.getType() == TopicMessageEventDTO.TypeEnum.DONE) {
            assertThat(evt.getCursor()).isNull();
          }
        })
        .filter(evt -> evt.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE)
        .map(evt -> evt.getMessage().getValue());

    StepVerifier.create(remainingMsgs)
        .expectNextCount(msgsToGenerate - pageSize)
        .verifyComplete();
  }

  private void createTopicWithCleanup(NewTopic newTopic) {
    createTopic(newTopic);
    createdTopics.add(newTopic.name());
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

  @Test
  void sendMessageWithProtobufAnyType() {
    String jsonContent = """
        {
          "name": "testName",
          "payload": {
            "@type": "type.googleapis.com/test.PayloadMessage",
            "id": "123"
          }
        }
        """;

    CreateTopicMessageDTO testMessage = new CreateTopicMessageDTO()
        .key(null)
        .partition(0)
        .keySerde(StringSerde.NAME)
        .value(jsonContent)
        .valueSerde(ProtobufFileSerde.NAME);

    String testTopic = MASKED_TOPICS_PREFIX + UUID.randomUUID();
    createTopicWithCleanup(new NewTopic(testTopic, 5, (short) 1));

    StepVerifier.create(messagesService.sendMessage(cluster, testTopic, testMessage))
        .expectNextMatches(metadata -> metadata.topic().equals(testTopic)
            && metadata.partition() == 0
            && metadata.offset() >= 0)
        .verifyComplete();
  }

}
