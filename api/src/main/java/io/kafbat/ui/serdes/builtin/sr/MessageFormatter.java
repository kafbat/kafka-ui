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
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.kafbat.ui.util.jsonschema.JsonAvroConversion;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;

interface MessageFormatter {

  String format(String topic, byte[] value);

  static Map<SchemaType, MessageFormatter> createMap(SchemaRegistryClient schemaRegistryClient,
                                                     boolean gcpSchemaRegistry) {
    return Map.of(
        SchemaType.AVRO, new AvroMessageFormatter(schemaRegistryClient, gcpSchemaRegistry),
        SchemaType.JSON, new JsonSchemaMessageFormatter(schemaRegistryClient),
        SchemaType.PROTOBUF, new ProtobufMessageFormatter(schemaRegistryClient)
    );
  }

  class AvroMessageFormatter implements MessageFormatter {
    private final KafkaAvroDeserializer avroDeserializer;

    AvroMessageFormatter(SchemaRegistryClient client, boolean gcpSchemaRegistry) {
      this.avroDeserializer = new KafkaAvroDeserializer(client);

      final Map<String, Object> avroProps = new HashMap<>();
      avroProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "wontbeused");
      avroProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, false);
      avroProps.put(KafkaAvroDeserializerConfig.SCHEMA_REFLECTION_CONFIG, false);
      avroProps.put(KafkaAvroDeserializerConfig.AVRO_USE_LOGICAL_TYPE_CONVERTERS_CONFIG, true);

      if (gcpSchemaRegistry) {
        avroProps.put(KafkaAvroDeserializerConfig.BEARER_AUTH_CREDENTIALS_SOURCE, "CUSTOM");
        avroProps.put(KafkaAvroDeserializerConfig.BEARER_AUTH_CUSTOM_PROVIDER_CLASS,
            "class com.google.cloud.hosted.kafka.auth.GcpBearerAuthCredentialProvider");
      }

      this.avroDeserializer.configure(avroProps, false);

    }

    @Override
    public String format(String topic, byte[] value) {
      Object deserialized = avroDeserializer.deserialize(topic, value);
      var schema = AvroSchemaUtils.getSchema(deserialized);
      return JsonAvroConversion.convertAvroToJson(deserialized, schema).toString();
    }
  }

  class ProtobufMessageFormatter implements MessageFormatter {
    private final KafkaProtobufDeserializer<?> protobufDeserializer;

    ProtobufMessageFormatter(SchemaRegistryClient client) {
      this.protobufDeserializer = new KafkaProtobufDeserializer<>(client);
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

    JsonSchemaMessageFormatter(SchemaRegistryClient client) {
      this.jsonSchemaDeserializer = new KafkaJsonSchemaDeserializer<>(client);
    }

    @Override
    public String format(String topic, byte[] value) {
      JsonNode json = jsonSchemaDeserializer.deserialize(topic, value);
      return json.toString();
    }
  }
}
