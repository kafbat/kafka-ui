package io.kafbat.ui.serdes.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

class TransactionStateSerdeTest extends AbstractIntegrationTest {

  private static String targetTopic;
  private static String transactionalId;

  @BeforeAll
  static void createTopicAndCommitItsOffset() {
    targetTopic = TransactionStateSerdeTest.class.getSimpleName() + "-" + UUID.randomUUID();
    transactionalId = TransactionStateSerdeTest.class.getSimpleName() + "-" + UUID.randomUUID();
    createTopic(new NewTopic(targetTopic, 1, (short) 1));

    try (var producer = createTransactionalProducer(transactionalId)) {
      producer.initTransactions();

      producer.beginTransaction();
      producer.send(new ProducerRecord<>(targetTopic, "key1", "value1"));
      producer.commitTransaction();

      producer.beginTransaction();
      producer.send(new ProducerRecord<>(targetTopic, "key2", "value2"));
      producer.abortTransaction();
    }
  }

  @AfterAll
  static void cleanUp() {
    deleteTopic(targetTopic);
  }

  @Test
  void canOnlyDeserializeConsumerOffsetsTopic() {
    var serde = new TransactionStateSerde();
    assertThat(serde.canDeserialize(TransactionStateSerde.TOPIC_NAME, Serde.Target.KEY)).isTrue();
    assertThat(serde.canDeserialize(TransactionStateSerde.TOPIC_NAME, Serde.Target.VALUE)).isTrue();
    assertThat(serde.canDeserialize("anyOtherTopic", Serde.Target.KEY)).isFalse();
    assertThat(serde.canDeserialize("anyOtherTopic", Serde.Target.VALUE)).isFalse();
  }

  @Test
  void deserializesMessagesMadeByConsumerActivity() {
    var serde = new TransactionStateSerde();
    var keyDeserializer = serde.deserializer(TransactionStateSerde.TOPIC_NAME, Serde.Target.KEY);
    var valueDeserializer = serde.deserializer(TransactionStateSerde.TOPIC_NAME, Serde.Target.VALUE);

    try (var consumer = createConsumer()) {
      consumer.subscribe(List.of(TransactionStateSerde.TOPIC_NAME));

      List<Map<String, Object>> values = new ArrayList<>();

      Awaitility.await()
          .pollInSameThread()
          .atMost(Duration.ofMinutes(1))
          .untilAsserted(() -> {
            for (var rec : consumer.poll(Duration.ofMillis(200))) {
              if (rec.key() == null || rec.value() == null) {
                continue;
              }
              System.out.println("record");
              var keyJson = toMapFromJsom(keyDeserializer.deserialize(null, rec.key().get()));

              if (!keyJson.containsKey("transaction_id")) {
                continue;
              }

              var valueJson = toMapFromJsom(valueDeserializer.deserialize(null, rec.value().get()));
              values.add(valueJson);
            }

            assertThat(values.isEmpty()).isFalse();

            // check value structure
            assertThat(values).allSatisfy(v -> {
              assertThat(v).containsKeys(
                  "producer_id",
                  "producer_epoch",
                  "transaction_timeout_ms",
                  "transaction_status",
                  "transaction_last_update_timestamp_ms",
                  "transaction_start_timestamp_ms"
              );
            });

            // checking existence different states
            // 0 - Empty
            assertThat(values).anyMatch(v ->
                TransactionStateSerde.TransactionStatus.EMPTY.name().equals(v.get("transaction_status"))
                    && v.get("transaction_partitions") == null
            );

            // 1 - Ongoing
            assertThat(values).anyMatch(v ->
                TransactionStateSerde.TransactionStatus.ONGOING.name().equals(v.get("transaction_status"))
                    && v.get("transaction_partitions") instanceof List<?> list
                    && !list.isEmpty()
            );

            // 4 - CompleteCommit
            assertThat(values).anyMatch(v ->
                TransactionStateSerde.TransactionStatus.COMPLETE_COMMIT.name().equals(v.get("transaction_status"))
                    && v.get("transaction_partitions") instanceof List<?> list
                    && list.isEmpty()
            );

            // 5 - CompleteAbort
            assertThat(values).anyMatch(v ->
                TransactionStateSerde.TransactionStatus.COMPLETE_ABORT.name().equals(v.get("transaction_status"))
                    && v.get("transaction_partitions") instanceof List<?> list
                    && list.isEmpty()
            );
          });
    }
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, Object> toMapFromJsom(DeserializeResult result) {
    return new JsonMapper().readValue(result.getResult(), Map.class);
  }

  // message examples
  //  key:
  //  {
  //    "transaction_id": "my_ksql_1"
  //  }
  //  value:
  //  {
  //    "producer_id": 1,
  //      "producer_epoch": 0,
  //      "transaction_timeout_ms": 60000,
  //      "transaction_status": "COMPLETE_COMMIT",
  //      "transaction_partitions": [],
  //    "transaction_last_update_timestamp_ms": 1781366899882,
  //      "transaction_start_timestamp_ms": 1781366899725
  //  }
  private static KafkaConsumer<Bytes, Bytes> createConsumer() {
    Properties props = new Properties();
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "transaction-state-" + UUID.randomUUID());
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BytesDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new KafkaConsumer<>(props);
  }

  private static KafkaProducer<String, String> createTransactionalProducer(String transactionalId) {
    return new KafkaProducer<>(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionalId,
        //use transactions
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"
    ));
  }
}
