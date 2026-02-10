package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.producer.KafkaTestProducer;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import io.kafbat.ui.serdes.PropertyResolverImpl;
import io.kafbat.ui.serdes.builtin.StringSerde;
import io.kafbat.ui.service.PollingCursorsStorage;
import io.kafbat.ui.util.ApplicationMetrics;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;


class CursorTest extends AbstractIntegrationTest {

  static final String TOPIC = CursorTest.class.getSimpleName() + "_" + UUID.randomUUID();
  static final int MSGS_IN_PARTITION = 20;
  static final int PAGE_SIZE = (MSGS_IN_PARTITION / 2) + 1; //to poll fill data set in 2 iterations

  final PollingCursorsStorage cursorsStorage = new PollingCursorsStorage();

  @BeforeAll
  static void setup() {
    createTopic(new NewTopic(TOPIC, 1, (short) 1));
    try (var producer = KafkaTestProducer.forKafka(kafka)) {
      for (int i = 0; i < MSGS_IN_PARTITION; i++) {
        producer.send(new ProducerRecord<>(TOPIC, "msg_" + i));
      }
    }
  }

  @AfterAll
  static void cleanup() {
    deleteTopic(TOPIC);
  }

  @Test
  void backwardEmitter() {
    var consumerPosition = new ConsumerPosition(PollingModeDTO.LATEST, TOPIC, List.of(), null, null);
    var emitter = createBackwardEmitter(consumerPosition);
    waitMgsgEmitted(emitter, PAGE_SIZE);
    var cursor = assertCursor(
        PollingModeDTO.TO_OFFSET,
        offsets -> assertThat(offsets)
            .hasSize(1)
            .containsEntry(new TopicPartition(TOPIC, 0), 9L)
    );

    // polling remaining records using registered cursor
    emitter = createBackwardEmitterWithCursor(cursor);
    waitMgsgEmitted(emitter, MSGS_IN_PARTITION - PAGE_SIZE);
    //checking no new cursors registered
    assertThat(cursorsStorage.asMap()).hasSize(1).containsValue(cursor);
  }

  @Test
  void forwardEmitterWithZeroLimitCompletesWithoutHanging() {
    var consumerPosition = new ConsumerPosition(PollingModeDTO.EARLIEST, TOPIC, List.of(), null, null);
    var emitter = new ForwardEmitter(
        this::createConsumer,
        consumerPosition,
        0,
        createRecordsDeserializer(),
        _ -> true,
        PollingSettings.createDefault(),
        cursorsStorage.createNewCursor(createRecordsDeserializer(), consumerPosition, _ -> true, PAGE_SIZE)
    );
    List<TopicMessageEventDTO> events = Flux.create(emitter).collectList().block();
    assertThat(events).isNotNull();
    assertThat(events.stream().filter(m -> m.getType() == TopicMessageEventDTO.TypeEnum.DONE).count()).isEqualTo(1);
  }

  @Test
  void backwardEmitterWithZeroLimitCompletesWithoutHanging() {
    var consumerPosition = new ConsumerPosition(PollingModeDTO.LATEST, TOPIC, List.of(), null, null);
    var emitter = new BackwardEmitter(
        this::createConsumer,
        consumerPosition,
        0,
        createRecordsDeserializer(),
        _ -> true,
        PollingSettings.createDefault(),
        cursorsStorage.createNewCursor(createRecordsDeserializer(), consumerPosition, _ -> true, PAGE_SIZE)
    );
    List<TopicMessageEventDTO> events = Flux.create(emitter).collectList().block();
    assertThat(events).isNotNull();
    assertThat(events.stream().filter(m -> m.getType() == TopicMessageEventDTO.TypeEnum.DONE).count()).isEqualTo(1);
  }

  @Test
  void forwardEmitter() {
    var consumerPosition = new ConsumerPosition(PollingModeDTO.EARLIEST, TOPIC, List.of(), null, null);
    var emitter = createForwardEmitter(consumerPosition);
    waitMgsgEmitted(emitter, PAGE_SIZE);
    var cursor = assertCursor(
        PollingModeDTO.FROM_OFFSET,
        offsets -> assertThat(offsets)
            .hasSize(1)
            .containsEntry(new TopicPartition(TOPIC, 0), 11L)
    );

    //polling remaining records using registered cursor
    emitter = createForwardEmitterWithCursor(cursor);
    waitMgsgEmitted(emitter, MSGS_IN_PARTITION - PAGE_SIZE);
    //checking no new cursors registered
    assertThat(cursorsStorage.asMap()).hasSize(1).containsValue(cursor);
  }

  private Cursor assertCursor(PollingModeDTO expectedMode,
                              Consumer<Map<TopicPartition, Long>> offsetsAssert) {
    Cursor registeredCursor = cursorsStorage.asMap().values().stream().findFirst().orElse(null);
    assertThat(registeredCursor).isNotNull();
    assertThat(registeredCursor.limit()).isEqualTo(PAGE_SIZE);
    assertThat(registeredCursor.deserializer()).isNotNull();
    assertThat(registeredCursor.filter()).isNotNull();

    var cursorPosition = registeredCursor.consumerPosition();
    assertThat(cursorPosition).isNotNull();
    assertThat(cursorPosition.offsets()).isNotNull();
    assertThat(cursorPosition.topic()).isEqualTo(TOPIC);
    assertThat(cursorPosition.partitions()).isEqualTo(List.of());
    assertThat(cursorPosition.pollingMode()).isEqualTo(expectedMode);

    offsetsAssert.accept(cursorPosition.offsets().tpOffsets());
    return registeredCursor;
  }

  private void waitMgsgEmitted(AbstractEmitter emitter, int expectedMsgsCnt) {
    List<TopicMessageEventDTO> events = Flux.create(emitter)
        .collectList()
        .block();
    assertThat(events).isNotNull();
    assertThat(events.stream().filter(m -> m.getType() == TopicMessageEventDTO.TypeEnum.MESSAGE).count())
        .isEqualTo(expectedMsgsCnt);
  }

  private BackwardEmitter createBackwardEmitter(ConsumerPosition position) {
    return new BackwardEmitter(
        this::createConsumer,
        position,
        PAGE_SIZE,
        createRecordsDeserializer(),
        _ -> true,
        PollingSettings.createDefault(),
        createCursor(position)
    );
  }

  private BackwardEmitter createBackwardEmitterWithCursor(Cursor cursor) {
    return new BackwardEmitter(
        this::createConsumer,
        cursor.consumerPosition(),
        cursor.limit(),
        cursor.deserializer(),
        cursor.filter(),
        PollingSettings.createDefault(),
        createCursor(cursor.consumerPosition())
    );
  }

  private ForwardEmitter createForwardEmitterWithCursor(Cursor cursor) {
    return new ForwardEmitter(
        this::createConsumer,
        cursor.consumerPosition(),
        cursor.limit(),
        cursor.deserializer(),
        cursor.filter(),
        PollingSettings.createDefault(),
        createCursor(cursor.consumerPosition())
    );
  }

  private ForwardEmitter createForwardEmitter(ConsumerPosition position) {
    return new ForwardEmitter(
        this::createConsumer,
        position,
        PAGE_SIZE,
        createRecordsDeserializer(),
        _ -> true,
        PollingSettings.createDefault(),
        createCursor(position)
    );
  }

  private Cursor.Tracking createCursor(ConsumerPosition position) {
    return cursorsStorage.createNewCursor(createRecordsDeserializer(), position, _ -> true, PAGE_SIZE);
  }

  private EnhancedConsumer createConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, PAGE_SIZE - 1); // to check multiple polls
    return new EnhancedConsumer(props, PollingThrottler.noop(), ApplicationMetrics.noop());
  }

  private static ConsumerRecordDeserializer createRecordsDeserializer() {
    Serde s = new StringSerde();
    s.configure(PropertyResolverImpl.empty(), PropertyResolverImpl.empty(), PropertyResolverImpl.empty());
    return new ConsumerRecordDeserializer(
        StringSerde.NAME,
        s.deserializer(null, Serde.Target.KEY),
        StringSerde.NAME,
        s.deserializer(null, Serde.Target.VALUE),
        StringSerde.NAME,
        s.deserializer(null, Serde.Target.KEY),
        s.deserializer(null, Serde.Target.VALUE),
        msg -> msg
    );
  }

}
