package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.CreateTopicMessageDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.serdes.builtin.Int32Serde;
import io.kafbat.ui.serdes.builtin.Int64Serde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import io.kafbat.ui.serdes.builtin.sr.SchemaRegistrySerde;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class SendAndReadTests extends AbstractIntegrationTest {

  private static final AvroSchema AVRO_SCHEMA_1 = new AvroSchema(
      "{"
          + "  \"type\": \"record\","
          + "  \"name\": \"TestAvroRecord1\","
          + "  \"fields\": ["
          + "    {"
          + "      \"name\": \"field1\","
          + "      \"type\": \"string\""
          + "    },"
          + "    {"
          + "      \"name\": \"field2\","
          + "      \"type\": \"int\""
          + "    }"
          + "  ]"
          + "}"
  );

  private static final AvroSchema AVRO_SCHEMA_2 = new AvroSchema(
      "{"
          + "  \"type\": \"record\","
          + "  \"name\": \"TestAvroRecord2\","
          + "  \"fields\": ["
          + "    {"
          + "      \"name\": \"f1\","
          + "      \"type\": \"int\""
          + "    },"
          + "    {"
          + "      \"name\": \"f2\","
          + "      \"type\": \"string\""
          + "    }"
          + "  ]"
          + "}"
  );

  private static final AvroSchema AVRO_SCHEMA_PRIMITIVE_STRING =
      new AvroSchema("{ \"type\": \"string\" }");

  private static final AvroSchema AVRO_SCHEMA_PRIMITIVE_INT =
      new AvroSchema("{ \"type\": \"int\" }");


  private static final String AVRO_SCHEMA_1_JSON_RECORD
      = "{ \"field1\":\"testStr\", \"field2\": 123 }";

  private static final String AVRO_SCHEMA_2_JSON_RECORD = "{ \"f1\": 111, \"f2\": \"testStr\" }";

  private static final ProtobufSchema PROTOBUF_SCHEMA = new ProtobufSchema(
      """
          syntax = "proto3";
          package io.kafbat;

          message TestProtoRecord {
            string f1 = 1;
            int32 f2 = 2;
          }

          """
  );

  private static final String PROTOBUF_SCHEMA_JSON_RECORD
      = "{ \"f1\" : \"test str\", \"f2\" : 123 }";


  private static final JsonSchema JSON_SCHEMA = new JsonSchema(
      "{ "
          + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\", "
          + "  \"$id\": \"http://example.com/myURI.schema.json\", "
          + "  \"title\": \"TestRecord\","
          + "  \"type\": \"object\","
          + "  \"additionalProperties\": false,"
          + "  \"properties\": {"
          + "    \"f1\": {"
          + "      \"type\": \"integer\""
          + "    },"
          + "    \"f2\": {"
          + "      \"type\": \"string\""
          + "    },"
          // it is important special case since there is code in KafkaJsonSchemaSerializer
          // that checks fields with this name (it should be worked around)
          + "    \"schema\": {"
          + "      \"type\": \"string\""
          + "    }"
          + "  }"
          + "}"
  );

  private static final String JSON_SCHEMA_RECORD
      = "{ \"f1\": 12, \"f2\": \"testJsonSchema1\", \"schema\": \"some txt\" }";

  private KafkaCluster targetCluster;

  @Autowired
  private MessagesService messagesService;

  @Autowired
  private ClustersStorage clustersStorage;

  @BeforeEach
  void init() {
    targetCluster = clustersStorage.getClusterByName(LOCAL).orElseThrow();
  }

  @Test
  void noSchemaStringKeyStringValue() {
    new SendAndReadSpec()
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("testKey")
                .keySerde(StringSerde.NAME)
                .value("testValue")
                .valueSerde(StringSerde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("testKey");
          assertThat(polled.getValue()).isEqualTo("testValue");
        });
  }

  @Test
  void keyIsIntValueIsLong() {
    new SendAndReadSpec()
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("123")
                .keySerde(Int32Serde.NAME)
                .value("21474836470")
                .valueSerde(Int64Serde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("123");
          assertThat(polled.getValue()).isEqualTo("21474836470");
        });
  }

  @Test
  void keyIsNull() {
    new SendAndReadSpec()
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(null)
                .keySerde(StringSerde.NAME)
                .value("testValue")
                .valueSerde(StringSerde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isNull();
          assertThat(polled.getValue()).isEqualTo("testValue");
        });
  }

  @Test
  void valueIsNull() {
    new SendAndReadSpec()
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("testKey")
                .keySerde(StringSerde.NAME)
                .value(null)
                .valueSerde(StringSerde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("testKey");
          assertThat(polled.getValue()).isNull();
        });
  }

  @Test
  void primitiveAvroSchemas() {
    new SendAndReadSpec()
        .withKeySchema(AVRO_SCHEMA_PRIMITIVE_STRING)
        .withValueSchema(AVRO_SCHEMA_PRIMITIVE_INT)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("\"some string\"")
                .keySerde(SchemaRegistrySerde.NAME)
                .value("123")
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("\"some string\"");
          assertThat(polled.getValue()).isEqualTo("123");
        });
  }

  @Test
  void recordAvroSchema() {
    new SendAndReadSpec()
        .withKeySchema(AVRO_SCHEMA_1)
        .withValueSchema(AVRO_SCHEMA_2)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(AVRO_SCHEMA_1_JSON_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(AVRO_SCHEMA_2_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), AVRO_SCHEMA_1_JSON_RECORD);
          assertJsonEqual(polled.getValue(), AVRO_SCHEMA_2_JSON_RECORD);
        });
  }

  @Test
  void keyWithNoSchemaValueWithProtoSchema() {
    new SendAndReadSpec()
        .withValueSchema(PROTOBUF_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("testKey")
                .keySerde(StringSerde.NAME)
                .value(PROTOBUF_SCHEMA_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("testKey");
          assertJsonEqual(polled.getValue(), PROTOBUF_SCHEMA_JSON_RECORD);
        });
  }

  @Test
  void keyWithAvroSchemaValueWithAvroSchemaKeyIsNull() {
    new SendAndReadSpec()
        .withKeySchema(AVRO_SCHEMA_1)
        .withValueSchema(AVRO_SCHEMA_2)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(null)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(AVRO_SCHEMA_2_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)

        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isNull();
          assertJsonEqual(polled.getValue(), AVRO_SCHEMA_2_JSON_RECORD);
        });
  }

  @Test
  void valueWithAvroSchemaShouldThrowExceptionIfArgIsNotValidJsonObject() {
    new SendAndReadSpec()
        .withValueSchema(AVRO_SCHEMA_2)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .keySerde(StringSerde.NAME)
                // f2 has type int instead of string
                .value("{ \"f1\": 111, \"f2\": 123 }")
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .assertSendThrowsException();
  }

  @Test
  void keyWithAvroSchemaValueWithProtoSchema() {
    new SendAndReadSpec()
        .withKeySchema(AVRO_SCHEMA_1)
        .withValueSchema(PROTOBUF_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(AVRO_SCHEMA_1_JSON_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(PROTOBUF_SCHEMA_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), AVRO_SCHEMA_1_JSON_RECORD);
          assertJsonEqual(polled.getValue(), PROTOBUF_SCHEMA_JSON_RECORD);
        });
  }

  @Test
  void valueWithProtoSchemaShouldThrowExceptionArgIsNotValidJsonObject() {
    new SendAndReadSpec()
        .withValueSchema(PROTOBUF_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(null)
                .keySerde(StringSerde.NAME)
                // f2 field has type object instead of int
                .value("{ \"f1\" : \"test str\", \"f2\" : {} }")
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .assertSendThrowsException();
  }

  @Test
  void keyWithProtoSchemaValueWithJsonSchema() {
    new SendAndReadSpec()
        .withKeySchema(PROTOBUF_SCHEMA)
        .withValueSchema(JSON_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(PROTOBUF_SCHEMA_JSON_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(JSON_SCHEMA_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), PROTOBUF_SCHEMA_JSON_RECORD);
          assertJsonEqual(polled.getValue(), JSON_SCHEMA_RECORD);
        });
  }

  @Test
  void valueWithJsonSchemaThrowsExceptionIfArgIsNotValidJsonObject() {
    new SendAndReadSpec()
        .withValueSchema(JSON_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(null)
                .keySerde(StringSerde.NAME)
                // 'f2' field has type object instead of string
                .value("{ \"f1\": 12, \"f2\": {}, \"schema\": \"some txt\" }")
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .assertSendThrowsException();
  }

  @Test
  void topicMessageMetadataAvro() {
    new SendAndReadSpec()
        .withKeySchema(AVRO_SCHEMA_1)
        .withValueSchema(AVRO_SCHEMA_2)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(AVRO_SCHEMA_1_JSON_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(AVRO_SCHEMA_2_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), AVRO_SCHEMA_1_JSON_RECORD);
          assertJsonEqual(polled.getValue(), AVRO_SCHEMA_2_JSON_RECORD);
          assertThat(polled.getKeySize()).isEqualTo(15L);
          assertThat(polled.getValueSize()).isEqualTo(15L);
          assertThat(polled.getKeyDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getKeyDeserializeProperties().get("type")).isEqualTo("AVRO");
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("AVRO");
        });
  }

  @Test
  void topicMessageMetadataProtobuf() {
    new SendAndReadSpec()
        .withKeySchema(PROTOBUF_SCHEMA)
        .withValueSchema(PROTOBUF_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(PROTOBUF_SCHEMA_JSON_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(PROTOBUF_SCHEMA_JSON_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), PROTOBUF_SCHEMA_JSON_RECORD);
          assertJsonEqual(polled.getValue(), PROTOBUF_SCHEMA_JSON_RECORD);
          assertThat(polled.getKeySize()).isEqualTo(18L);
          assertThat(polled.getValueSize()).isEqualTo(18L);
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getKeyDeserializeProperties().get("type")).isEqualTo("PROTOBUF");
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("PROTOBUF");
        });
  }

  @Test
  void topicMessageMetadataJson() {
    new SendAndReadSpec()
        .withKeySchema(JSON_SCHEMA)
        .withValueSchema(JSON_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(JSON_SCHEMA_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(JSON_SCHEMA_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
                .headers(Map.of("header1", "value1"))
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), JSON_SCHEMA_RECORD);
          assertJsonEqual(polled.getValue(), JSON_SCHEMA_RECORD);
          assertThat(polled.getKeySize()).isEqualTo(57L);
          assertThat(polled.getValueSize()).isEqualTo(57L);
          assertThat(polled.getHeadersSize()).isEqualTo(13L);
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getKeyDeserializeProperties().get("type")).isEqualTo("JSON");
          assertThat(polled.getValueDeserializeProperties().get("id")).isNotNull();
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("JSON");
        });
  }

  @Test
  void headerValueNullPresentTest() {
    new SendAndReadSpec()
        .withKeySchema(JSON_SCHEMA)
        .withValueSchema(JSON_SCHEMA)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(JSON_SCHEMA_RECORD)
                .keySerde(SchemaRegistrySerde.NAME)
                .value(JSON_SCHEMA_RECORD)
                .valueSerde(SchemaRegistrySerde.NAME)
                .headers(Collections.singletonMap("header123", null))
        )
        .doAssert(polled -> assertThat(polled.getHeaders().get("header123")).isNull());
  }


  @Test
  void noKeyAndNoContentPresentTest() {
    new SendAndReadSpec()
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(null)
                .keySerde(StringSerde.NAME) // any serde
                .value(null)
                .valueSerde(StringSerde.NAME) // any serde
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isNull();
          assertThat(polled.getValue()).isNull();
        });
  }

  // Explicit subject tests for TopicRecordNameStrategy and RecordNameStrategy
  private static final AvroSchema AVRO_RECORD_SCHEMA = new AvroSchema(
      """
          {
            "type": "record",
            "name": "TestRecord",
            "fields": [
              {"name": "field1", "type": "string"},
              {"name": "field2", "type": "int"}
            ]
          }
          """
  );

  private static final ProtobufSchema EXPLICIT_PROTOBUF_SCHEMA = new ProtobufSchema(
      """
          syntax = "proto3";
          package io.kafbat.test;

          message ExplicitTestRecord {
            string field1 = 1;
            int32 field2 = 2;
          }
          """
  );

  private static final JsonSchema EXPLICIT_JSON_SCHEMA = new JsonSchema(
      """
          {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "title": "ExplicitTestRecord",
            "type": "object",
            "properties": {
              "field1": { "type": "string" },
              "field2": { "type": "integer" }
            }
          }
          """
  );

  private static final String RECORD_JSON = "{ \"field1\": \"test\", \"field2\": 42 }";

  @Test
  void sendAvroMessageWithTopicRecordNameStrategySubject() {
    String valueSubject = "com.example.TopicRecordNameSubject";
    new SendAndReadSpec()
        .withValueSchema(AVRO_RECORD_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("test-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("test-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("AVRO");
        });
  }

  @Test
  void sendAvroMessageWithRecordNameStrategySubject() {
    String valueSubject = "io.kafbat.test.UserRecord";
    new SendAndReadSpec()
        .withValueSchema(AVRO_RECORD_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("test-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("test-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("AVRO");
        });
  }

  @Test
  void sendProtobufMessageWithTopicRecordNameStrategySubject() {
    String valueSubject = "events-UserCreated";
    new SendAndReadSpec()
        .withValueSchema(EXPLICIT_PROTOBUF_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("proto-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("proto-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("PROTOBUF");
        });
  }

  @Test
  void sendProtobufMessageWithRecordNameStrategySubject() {
    String valueSubject = "io.kafbat.test.ExplicitTestRecord";
    new SendAndReadSpec()
        .withValueSchema(EXPLICIT_PROTOBUF_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("proto-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("proto-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("PROTOBUF");
        });
  }

  @Test
  void sendJsonSchemaMessageWithTopicRecordNameStrategySubject() {
    String valueSubject = "orders-OrderCreated";
    new SendAndReadSpec()
        .withValueSchema(EXPLICIT_JSON_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("json-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("json-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("JSON");
        });
  }

  @Test
  void sendJsonSchemaMessageWithRecordNameStrategySubject() {
    String valueSubject = "com.example.JsonRecord";
    new SendAndReadSpec()
        .withValueSchema(EXPLICIT_JSON_SCHEMA)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key("json-key")
                .keySerde(StringSerde.NAME)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertThat(polled.getKey()).isEqualTo("json-key");
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("JSON");
        });
  }

  @Test
  void sendMessageWithDifferentStrategiesForKeyAndValue() {
    // Key uses RecordNameStrategy, Value uses TopicRecordNameStrategy
    String keySubject = "com.example.KeyRecord";
    String valueSubject = "events-ValueRecord";
    new SendAndReadSpec()
        .withKeySchema(AVRO_RECORD_SCHEMA)
        .withValueSchema(AVRO_RECORD_SCHEMA)
        .withExplicitKeySubject(keySubject)
        .withExplicitValueSubject(valueSubject)
        .withMsgToSend(
            new CreateTopicMessageDTO()
                .key(RECORD_JSON)
                .keySerde(SchemaRegistrySerde.NAME)
                .keySubject(keySubject)
                .value(RECORD_JSON)
                .valueSerde(SchemaRegistrySerde.NAME)
                .valueSubject(valueSubject)
        )
        .doAssert(polled -> {
          assertJsonEqual(polled.getKey(), RECORD_JSON);
          assertJsonEqual(polled.getValue(), RECORD_JSON);
          assertThat(polled.getKeyDeserializeProperties().get("type")).isEqualTo("AVRO");
          assertThat(polled.getValueDeserializeProperties().get("type")).isEqualTo("AVRO");
        });
  }

  @SneakyThrows
  private void assertJsonEqual(String actual, String expected) {
    var mapper = new ObjectMapper();
    assertThat(mapper.readTree(actual)).isEqualTo(mapper.readTree(expected));
  }

  class SendAndReadSpec {
    CreateTopicMessageDTO msgToSend;
    ParsedSchema keySchema;
    ParsedSchema valueSchema;
    String explicitKeySubject;
    String explicitValueSubject;

    public SendAndReadSpec withMsgToSend(CreateTopicMessageDTO msg) {
      this.msgToSend = msg;
      return this;
    }

    public SendAndReadSpec withKeySchema(ParsedSchema keyScheam) {
      this.keySchema = keyScheam;
      return this;
    }

    public SendAndReadSpec withValueSchema(ParsedSchema valueSchema) {
      this.valueSchema = valueSchema;
      return this;
    }

    public SendAndReadSpec withExplicitKeySubject(String subject) {
      this.explicitKeySubject = subject;
      return this;
    }

    public SendAndReadSpec withExplicitValueSubject(String subject) {
      this.explicitValueSubject = subject;
      return this;
    }

    @SneakyThrows
    private String createTopicAndCreateSchemas() {
      Objects.requireNonNull(msgToSend);
      String topic = UUID.randomUUID().toString();
      createTopic(new NewTopic(topic, 1, (short) 1));
      if (keySchema != null) {
        String keySubject = explicitKeySubject != null ? explicitKeySubject : topic + "-key";
        schemaRegistry.schemaRegistryClient().register(keySubject, keySchema);
      }
      if (valueSchema != null) {
        String valueSubject = explicitValueSubject != null ? explicitValueSubject : topic + "-value";
        schemaRegistry.schemaRegistryClient().register(valueSubject, valueSchema);
      }
      return topic;
    }

    public void assertSendThrowsException() {
      String topic = createTopicAndCreateSchemas();
      try {
        StepVerifier.create(
            messagesService.sendMessage(targetCluster, topic, msgToSend)
        ).expectError().verify();
      } finally {
        deleteTopic(topic);
      }
    }

    @SneakyThrows
    public void doAssert(Consumer<TopicMessageDTO> msgAssert) {
      String topic = createTopicAndCreateSchemas();
      try {
        messagesService.sendMessage(targetCluster, topic, msgToSend).block();
        TopicMessageDTO polled = messagesService.loadMessages(
                targetCluster,
                topic,
                new ConsumerPosition(PollingModeDTO.EARLIEST, topic, List.of(), null, null),
                null,
                null,
                1,
                msgToSend.getKeySerde().get(),
                msgToSend.getValueSerde().get()
            ).filter(e -> e.getType().equals(TopicMessageEventDTO.TypeEnum.MESSAGE))
            .map(TopicMessageEventDTO::getMessage)
            .blockLast(Duration.ofSeconds(5000));

        assertThat(polled).isNotNull();
        assertThat(polled.getPartition()).isZero();
        assertThat(polled.getOffset()).isNotNull();
        msgAssert.accept(polled);
      } finally {
        deleteTopic(topic);
      }
    }
  }

}
