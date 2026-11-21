package io.kafbat.ui.serdes.builtin.sr;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.kafbat.ui.util.jsonschema.JsonAvroConversion;
import java.util.Map;
import lombok.SneakyThrows;

interface MessageFormatter {

  String PLACEHOLDER_SCHEMA_REGISTRY_URL = "wontbeused";

  String format(String topic, byte[] value);

  static Map<SchemaType, MessageFormatter> createMap(SchemaRegistryClient schemaRegistryClient,
                                                     FormatterProperties formatterProperties,
                                                     boolean isKey) {
    return Map.of(
        SchemaType.AVRO, new AvroMessageFormatter(schemaRegistryClient, formatterProperties, isKey),
        SchemaType.JSON, new JsonSchemaMessageFormatter(schemaRegistryClient, isKey),
        SchemaType.PROTOBUF, new ProtobufMessageFormatter(schemaRegistryClient, isKey)
    );
  }

  class AvroMessageFormatter implements MessageFormatter {
    private final KafkaAvroDeserializer avroDeserializer;
    private final FormatterProperties properties;

    AvroMessageFormatter(SchemaRegistryClient client, FormatterProperties properties, boolean isKey) {
      this.properties = properties != null ? properties : FormatterProperties.EMPTY;
      this.avroDeserializer = new KafkaAvroDeserializer(client);
      this.avroDeserializer.configure(
          Map.of(
              AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, PLACEHOLDER_SCHEMA_REGISTRY_URL,
              KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false,
              KafkaAvroDeserializerConfig.SCHEMA_REFLECTION_CONFIG, false,
              KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true
          ),
          isKey
      );
    }

    @Override
    public String format(String topic, byte[] value) {
      Object deserialized = avroDeserializer.deserialize(topic, value);
      var schema = AvroSchemaUtils.getSchema(deserialized);
      return JsonAvroConversion.convertAvroToJson(deserialized, schema, properties).toString();
    }
  }

  class ProtobufMessageFormatter implements MessageFormatter {
    private final KafkaProtobufDeserializer<?> protobufDeserializer;

    ProtobufMessageFormatter(SchemaRegistryClient client, boolean isKey) {
      this.protobufDeserializer = new KafkaProtobufDeserializer<>(client);
      this.protobufDeserializer.configure(
          Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, PLACEHOLDER_SCHEMA_REGISTRY_URL),
          isKey
      );
    }

    @Override
    @SneakyThrows
    public String format(String topic, byte[] value) {
      final Message message = protobufDeserializer.deserialize(topic, value);
      return JsonFormat.printer()
          .includingDefaultValueFields()
          .omittingInsignificantWhitespace()
          .preservingProtoFieldNames()
          .print(message);
    }
  }

  class JsonSchemaMessageFormatter implements MessageFormatter {
    private final KafkaJsonSchemaDeserializer<JsonNode> jsonSchemaDeserializer;

    JsonSchemaMessageFormatter(SchemaRegistryClient client, boolean isKey) {
      this.jsonSchemaDeserializer = new KafkaJsonSchemaDeserializer<>(client);
      this.jsonSchemaDeserializer.configure(
          Map.of(
              AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, PLACEHOLDER_SCHEMA_REGISTRY_URL,
              KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE, JsonNode.class,
              KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, JsonNode.class
          ),
          isKey
      );
    }

    @Override
    public String format(String topic, byte[] value) {
      JsonNode json = jsonSchemaDeserializer.deserialize(topic, value);
      return json.toString();
    }
  }
}
