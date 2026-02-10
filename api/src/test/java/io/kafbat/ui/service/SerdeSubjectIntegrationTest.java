package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.SerdeUsageDTO;
import io.kafbat.ui.model.TopicSerdeSuggestionDTO;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the getSerdes endpoint covering all schema types
 * and naming strategies.
 */
class SerdeSubjectIntegrationTest extends AbstractIntegrationTest {

  private static final AvroSchema AVRO_SCHEMA = new AvroSchema(
      """
          {
            "type": "record",
            "name": "TestAvroRecord",
            "fields": [
              {"name": "field1", "type": "string"}
            ]
          }
          """
  );

  private static final ProtobufSchema PROTOBUF_SCHEMA = new ProtobufSchema(
      """
          syntax = "proto3";
          message TestProtoRecord {
            string field1 = 1;
          }
          """
  );

  private static final JsonSchema JSON_SCHEMA = new JsonSchema(
      """
          {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "properties": {
              "field1": { "type": "string" }
            }
          }
          """
  );

  @Autowired
  private WebTestClient webTestClient;

  private String topicName;

  @BeforeEach
  void setUp() {
    topicName = "test-serdes-" + UUID.randomUUID();
    createTopic(new NewTopic(topicName, 1, (short) 1));
  }

  @AfterEach
  void tearDown() {
    deleteTopic(topicName);
    cleanupSubjects();
  }

  @SneakyThrows
  private void cleanupSubjects() {
    var subjects = schemaRegistry.schemaRegistryClient().getAllSubjects();
    for (String subject : subjects) {
      if (subject.contains(topicName) || subject.startsWith("com.example.") || subject.startsWith("io.kafbat.")) {
        try {
          schemaRegistry.schemaRegistryClient().deleteSubject(subject);
        } catch (Exception ignored) {
          // ignore cleanup errors
        }
      }
    }
  }

  private TopicSerdeSuggestionDTO getSerdes(SerdeUsageDTO usage) {
    return webTestClient
        .get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/serdes?use={usage}",
            LOCAL, topicName, usage.getValue())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TopicSerdeSuggestionDTO.class)
        .returnResult()
        .getResponseBody();
  }

  @Nested
  class TopicNameStrategyTests {

    @Test
    @SneakyThrows
    void getSerdesReturnsAvroSubjectsForTopicNameStrategy() {
      schemaRegistry.schemaRegistryClient().register(topicName + "-key", AVRO_SCHEMA);
      schemaRegistry.schemaRegistryClient().register(topicName + "-value", AVRO_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var keySrSerde = result.getKey().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(keySrSerde).isPresent();
      assertThat(keySrSerde.get().getSubjects()).contains(topicName + "-key");

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(topicName + "-value");
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsProtobufSubjectsForTopicNameStrategy() {
      schemaRegistry.schemaRegistryClient().register(topicName + "-key", PROTOBUF_SCHEMA);
      schemaRegistry.schemaRegistryClient().register(topicName + "-value", PROTOBUF_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(topicName + "-value");
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsJsonSchemaSubjectsForTopicNameStrategy() {
      schemaRegistry.schemaRegistryClient().register(topicName + "-value", JSON_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(topicName + "-value");
    }
  }

  @Nested
  class TopicRecordNameStrategyTests {

    @Test
    @SneakyThrows
    void getSerdesReturnsAvroSubjectsForTopicRecordNameStrategy() {
      // TopicRecordNameStrategy: topic-RecordName
      String subject = topicName + "-OrderCreated";
      schemaRegistry.schemaRegistryClient().register(subject, AVRO_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsProtobufSubjectsForTopicRecordNameStrategy() {
      String subject = topicName + "-UserCreated";
      schemaRegistry.schemaRegistryClient().register(subject, PROTOBUF_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsJsonSchemaSubjectsForTopicRecordNameStrategy() {
      String subject = topicName + "-EventProcessed";
      schemaRegistry.schemaRegistryClient().register(subject, JSON_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }
  }

  @Nested
  class RecordNameStrategyTests {

    @Test
    @SneakyThrows
    void getSerdesReturnsAvroSubjectsForRecordNameStrategy() {
      // RecordNameStrategy: fully qualified name (no -key or -value suffix)
      String subject = "com.example.AvroRecord";
      schemaRegistry.schemaRegistryClient().register(subject, AVRO_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsProtobufSubjectsForRecordNameStrategy() {
      String subject = "io.kafbat.test.ProtoRecord";
      schemaRegistry.schemaRegistryClient().register(subject, PROTOBUF_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }

    @Test
    @SneakyThrows
    void getSerdesReturnsJsonSchemaSubjectsForRecordNameStrategy() {
      String subject = "com.example.JsonRecord";
      schemaRegistry.schemaRegistryClient().register(subject, JSON_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects()).contains(subject);
    }
  }

  @Nested
  class MixedScenarioTests {

    @Test
    @SneakyThrows
    void getSerdesReturnsSubjectsFromMultipleStrategies() {
      // Register subjects using all three strategies
      String topicNameSubject = topicName + "-value";
      String topicRecordNameSubject = topicName + "-OrderCreated";
      String recordNameSubject = "com.example.MixedRecord";

      schemaRegistry.schemaRegistryClient().register(topicNameSubject, AVRO_SCHEMA);
      schemaRegistry.schemaRegistryClient().register(topicRecordNameSubject, PROTOBUF_SCHEMA);
      schemaRegistry.schemaRegistryClient().register(recordNameSubject, JSON_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();

      assertThat(valueSrSerde).isPresent();
      List<String> subjects = valueSrSerde.get().getSubjects();

      assertThat(subjects).contains(topicNameSubject);
      assertThat(subjects).contains(topicRecordNameSubject);
      assertThat(subjects).contains(recordNameSubject);
    }

    @Test
    @SneakyThrows
    void getSerdesExcludesOppositeTypeSubjects() {
      // Register both key and value subjects
      String keySubject = topicName + "-key";
      String valueSubject = topicName + "-value";

      schemaRegistry.schemaRegistryClient().register(keySubject, AVRO_SCHEMA);
      schemaRegistry.schemaRegistryClient().register(valueSubject, AVRO_SCHEMA);

      var result = getSerdes(SerdeUsageDTO.SERIALIZE);

      assertThat(result).isNotNull();

      // Key subjects should not include -value subjects
      var keySrSerde = result.getKey().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();
      assertThat(keySrSerde).isPresent();
      assertThat(keySrSerde.get().getSubjects())
          .contains(keySubject)
          .doesNotContain(valueSubject);

      // Value subjects should not include -key subjects
      var valueSrSerde = result.getValue().stream()
          .filter(s -> "SchemaRegistry".equals(s.getName()))
          .findFirst();
      assertThat(valueSrSerde).isPresent();
      assertThat(valueSrSerde.get().getSubjects())
          .contains(valueSubject)
          .doesNotContain(keySubject);
    }
  }
}
