package io.kafbat.ui.serdes.builtin.sr;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeAvro;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeJson;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeProto;

import com.google.common.annotations.VisibleForTesting;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serdes.BuiltInSerde;
import io.kafbat.ui.service.ssl.SkipSecurityProvider;
import io.kafbat.ui.util.jsonschema.AvroJsonSchemaConverter;
import io.kafbat.ui.util.jsonschema.ProtobufSchemaConverter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.apache.kafka.common.config.SslConfigs;


public class SchemaRegistrySerde implements BuiltInSerde {
  public static final String NAME = "SchemaRegistry";
  private static final byte SR_PAYLOAD_MAGIC_BYTE = 0x0;
  private static final int SR_PAYLOAD_PREFIX_LENGTH = 5;

  private static final String SCHEMA_REGISTRY = "schemaRegistry";

  private SchemaRegistryClient schemaRegistryClient;
  private List<String> schemaRegistryUrls;
  private String valueSchemaNameTemplate;
  private String keySchemaNameTemplate;
  private boolean checkSchemaExistenceForDeserialize;

  private Map<SchemaType, MessageFormatter> schemaRegistryFormatters;

  @Override
  public boolean canBeAutoConfigured(PropertyResolver kafkaClusterProperties,
                                     PropertyResolver globalProperties) {
    return kafkaClusterProperties.getListProperty(SCHEMA_REGISTRY, String.class)
        .filter(lst -> !lst.isEmpty())
        .isPresent();
  }

  @Override
  public void autoConfigure(PropertyResolver kafkaClusterProperties,
                            PropertyResolver globalProperties) {
    var urls = kafkaClusterProperties.getListProperty(SCHEMA_REGISTRY, String.class)
        .filter(lst -> !lst.isEmpty())
        .orElseThrow(() -> new ValidationException("No urls provided for schema registry"));

    FormatterProperties.FormatterPropertiesBuilder propertiesBuilder = FormatterProperties.builder();
    kafkaClusterProperties.getProperty("schemaRegistryShowNullValues", Boolean.class)
        .ifPresent(propertiesBuilder::showNullValues);
    kafkaClusterProperties.getProperty("schemaRegistryUseFullyQualifiedNames", Boolean.class)
        .ifPresent(propertiesBuilder::fullyQualifiedNames);

    var formatterProperties = propertiesBuilder.build();

    configure(
        urls,
        createSchemaRegistryClient(
            urls,
            kafkaClusterProperties.getProperty("schemaRegistryAuth.username", String.class).orElse(null),
            kafkaClusterProperties.getProperty("schemaRegistryAuth.password", String.class).orElse(null),
            kafkaClusterProperties.getProperty("schemaRegistrySsl.keystoreLocation", String.class).orElse(null),
            kafkaClusterProperties.getProperty("schemaRegistrySsl.keystorePassword", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.truststoreLocation", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.truststorePassword", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.verify", Boolean.class).orElse(true)
        ),
        kafkaClusterProperties.getProperty("schemaRegistryKeySchemaNameTemplate", String.class).orElse("%s-key"),
        kafkaClusterProperties.getProperty("schemaRegistrySchemaNameTemplate", String.class).orElse("%s-value"),
        kafkaClusterProperties.getProperty("schemaRegistryCheckSchemaExistenceForDeserialize", Boolean.class)
            .orElse(false),
        formatterProperties
    );
  }

  @Override
  public void configure(PropertyResolver serdeProperties,
                        PropertyResolver kafkaClusterProperties,
                        PropertyResolver globalProperties) {
    var urls = serdeProperties.getListProperty("url", String.class)
        .or(() -> kafkaClusterProperties.getListProperty(SCHEMA_REGISTRY, String.class))
        .filter(lst -> !lst.isEmpty())
        .orElseThrow(() -> new ValidationException("No urls provided for schema registry"));

    FormatterProperties.FormatterPropertiesBuilder propertiesBuilder = FormatterProperties.builder();
    kafkaClusterProperties.getProperty("showNullValues", Boolean.class)
        .ifPresent(propertiesBuilder::showNullValues);
    kafkaClusterProperties.getProperty("useFullyQualifiedNames", Boolean.class)
        .ifPresent(propertiesBuilder::fullyQualifiedNames);

    var formatterProperties = propertiesBuilder.build();

    configure(
        urls,
        createSchemaRegistryClient(
            urls,
            serdeProperties.getProperty("username", String.class).orElse(null),
            serdeProperties.getProperty("password", String.class).orElse(null),
            serdeProperties.getProperty("keystoreLocation", String.class).orElse(null),
            serdeProperties.getProperty("keystorePassword", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.truststoreLocation", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.truststorePassword", String.class).orElse(null),
            kafkaClusterProperties.getProperty("ssl.verify", Boolean.class).orElse(true)
        ),
        serdeProperties.getProperty("keySchemaNameTemplate", String.class).orElse("%s-key"),
        serdeProperties.getProperty("schemaNameTemplate", String.class).orElse("%s-value"),
        serdeProperties.getProperty("checkSchemaExistenceForDeserialize", Boolean.class)
            .orElse(false),
        formatterProperties
    );
  }

  @VisibleForTesting
  void configure(
      List<String> schemaRegistryUrls,
      SchemaRegistryClient schemaRegistryClient,
      String keySchemaNameTemplate,
      String valueSchemaNameTemplate,
      boolean checkTopicSchemaExistenceForDeserialize) {
    configure(schemaRegistryUrls, schemaRegistryClient, keySchemaNameTemplate, valueSchemaNameTemplate,
        checkTopicSchemaExistenceForDeserialize, FormatterProperties.EMPTY);
  }

  @VisibleForTesting
  void configure(
      List<String> schemaRegistryUrls,
      SchemaRegistryClient schemaRegistryClient,
      String keySchemaNameTemplate,
      String valueSchemaNameTemplate,
      boolean checkTopicSchemaExistenceForDeserialize,
      FormatterProperties formatterProperties) {
    this.schemaRegistryUrls = schemaRegistryUrls;
    this.schemaRegistryClient = schemaRegistryClient;
    this.keySchemaNameTemplate = keySchemaNameTemplate;
    this.valueSchemaNameTemplate = valueSchemaNameTemplate;
    this.schemaRegistryFormatters = MessageFormatter.createMap(schemaRegistryClient, formatterProperties);
    this.checkSchemaExistenceForDeserialize = checkTopicSchemaExistenceForDeserialize;
  }

  private static SchemaRegistryClient createSchemaRegistryClient(List<String> urls,
                                                                 @Nullable String username,
                                                                 @Nullable String password,
                                                                 @Nullable String keyStoreLocation,
                                                                 @Nullable String keyStorePassword,
                                                                 @Nullable String trustStoreLocation,
                                                                 @Nullable String trustStorePassword,
                                                                 boolean verifySsl) {
    Map<String, String> configs = new HashMap<>();
    if (username != null && password != null) {
      configs.put(BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      configs.put(USER_INFO_CONFIG, username + ":" + password);
    } else if (username != null) {
      throw new ValidationException(
          "You specified username but do not specified password");
    } else if (password != null) {
      throw new ValidationException(
          "You specified password but do not specified username");
    }

    if (!verifySsl) {
      configs.put(
          SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTMANAGER_ALGORITHM_CONFIG,
          SkipSecurityProvider.NAME
      );
    }

    // We require at least a truststore. The logic is done similar to SchemaRegistryService.securedWebClientOnTLS
    if (trustStoreLocation != null && trustStorePassword != null) {
      configs.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
          trustStoreLocation);
      configs.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
          trustStorePassword);
    }

    if (keyStoreLocation != null && keyStorePassword != null) {
      configs.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
          keyStoreLocation);
      configs.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
          keyStorePassword);
      configs.put(SchemaRegistryClientConfig.CLIENT_NAMESPACE + SslConfigs.SSL_KEY_PASSWORD_CONFIG,
          keyStorePassword);
    }

    return new CachedSchemaRegistryClient(
        urls,
        1_000,
        List.of(new AvroSchemaProvider(), new ProtobufSchemaProvider(), new JsonSchemaProvider()),
        configs
    );
  }

  @Override
  public boolean canDeserialize(String topic, Target type) {
    String subject = schemaSubject(topic, type);
    return !checkSchemaExistenceForDeserialize
        || getSchemaBySubject(subject).isPresent();
  }

  @Override
  public boolean canSerialize(String topic, Target type) {
    return !getSchemaSubjects(topic, type).isEmpty();
  }

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Target type) {
    String subject = schemaSubject(topic, type);
    return getSchemaBySubject(subject)
        .flatMap(schemaMetadata ->
            //schema can be not-found, when schema contexts configured improperly
            getSchemaById(schemaMetadata.getId())
                .map(parsedSchema ->
                    new SchemaDescription(
                        convertSchema(schemaMetadata, parsedSchema),
                        Map.of(
                            "subject", subject,
                            "schemaId", schemaMetadata.getId(),
                            "latestVersion", schemaMetadata.getVersion(),
                            "type", schemaMetadata.getSchemaType() // AVRO / PROTOBUF / JSON
                        )
                    )));
  }

  @SneakyThrows
  private String convertSchema(SchemaMetadata schema, ParsedSchema parsedSchema) {
    URI basePath = new URI(schemaRegistryUrls.getFirst())
        .resolve(Integer.toString(schema.getId()));
    SchemaType schemaType = SchemaType.fromString(schema.getSchemaType())
        .orElseThrow(() -> new IllegalStateException("Unknown schema type: " + schema.getSchemaType()));
    return switch (schemaType) {
      case PROTOBUF -> new ProtobufSchemaConverter()
          .convert(basePath, ((ProtobufSchema) parsedSchema).toDescriptor())
          .toJson();
      case AVRO -> new AvroJsonSchemaConverter()
          .convert(basePath, ((AvroSchema) parsedSchema).rawSchema())
          .toJson();
      case JSON ->
          //need to use confluent JsonSchema since it includes resolved references
          ((JsonSchema) parsedSchema).rawSchema().toString();
    };
  }

  private Optional<ParsedSchema> getSchemaById(int id) {
    return wrapWith404Handler(() -> schemaRegistryClient.getSchemaById(id));
  }

  private Optional<SchemaMetadata> getSchemaBySubject(String subject) {
    return wrapWith404Handler(() -> schemaRegistryClient.getLatestSchemaMetadata(subject));
  }

  @SneakyThrows
  private <T> Optional<T> wrapWith404Handler(Callable<T> call) {
    try {
      return Optional.ofNullable(call.call());
    } catch (RestClientException restClientException) {
      if (restClientException.getStatus() == 404) {
        return Optional.empty();
      } else {
        throw new RuntimeException("Error calling SchemaRegistryClient", restClientException);
      }
    }
  }

  private String schemaSubject(String topic, Target type) {
    return String.format(type == Target.KEY ? keySchemaNameTemplate : valueSchemaNameTemplate, topic);
  }

  @SneakyThrows
  public List<String> getSchemaSubjects(String topic, Target type) {
    var allSubjects = schemaRegistryClient.getAllSubjects();
    if (allSubjects == null || allSubjects.isEmpty()) {
      return List.of();
    }

    String defaultSubject = schemaSubject(topic, type);
    String topicPrefix = topic + "-";
    // Exclude subjects for the opposite type
    String excludeSuffix = type == Target.KEY ? "-value" : "-key";

    return allSubjects.stream()
        .filter(subject -> {
          // Exclude subjects explicitly for the opposite type
          if (subject.endsWith(excludeSuffix)) {
            return false;
          }
          // TopicNameStrategy: exact match with default subject
          if (subject.equals(defaultSubject)) {
            return true;
          }
          // TopicRecordNameStrategy: starts with topic-
          if (subject.startsWith(topicPrefix)) {
            return true;
          }
          // RecordNameStrategy: doesn't end with -key or -value (not topic-based naming)
          if (!subject.endsWith("-key") && !subject.endsWith("-value")) {
            return true;
          }
          return false;
        })
        .toList();
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    String subject = schemaSubject(topic, type);
    SchemaMetadata meta = getSchemaBySubject(subject)
        .orElseThrow(() -> new ValidationException(
            String.format("No schema for subject '%s' found", subject)));
    ParsedSchema schema = getSchemaById(meta.getId())
        .orElseThrow(() -> new IllegalStateException(
            String.format("Schema found for id %s, subject '%s'", meta.getId(), subject)));
    SchemaType schemaType = SchemaType.fromString(meta.getSchemaType())
        .orElseThrow(() -> new IllegalStateException("Unknown schema type: " + meta.getSchemaType()));
    return switch (schemaType) {
      case PROTOBUF -> input ->
          serializeProto(schemaRegistryClient, topic, type, (ProtobufSchema) schema, meta.getId(), input);
      case AVRO -> input ->
          serializeAvro((AvroSchema) schema, meta.getId(), input);
      case JSON -> input ->
          serializeJson((JsonSchema) schema, meta.getId(), input);
    };
  }

  public Serializer serializerWithSubject(String topic, Target type, String explicitSubject) {
    SchemaMetadata meta = getSchemaBySubject(explicitSubject)
        .orElseThrow(() -> new ValidationException(
            String.format("No schema for subject '%s' found", explicitSubject)));
    ParsedSchema schema = getSchemaById(meta.getId())
        .orElseThrow(() -> new IllegalStateException(
            String.format("Schema not found for id %s, subject '%s'", meta.getId(), explicitSubject)));
    SchemaType schemaType = SchemaType.fromString(meta.getSchemaType())
        .orElseThrow(() -> new IllegalStateException("Unknown schema type: " + meta.getSchemaType()));
    return switch (schemaType) {
      case PROTOBUF -> input ->
          serializeProto(schemaRegistryClient, topic, type, (ProtobufSchema) schema, meta.getId(), input);
      case AVRO -> input ->
          serializeAvro((AvroSchema) schema, meta.getId(), input);
      case JSON -> input ->
          serializeJson((JsonSchema) schema, meta.getId(), input);
    };
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return (headers, data) -> {
      var schemaId = extractSchemaIdFromMsg(data);
      SchemaType format = getMessageFormatBySchemaId(schemaId);
      String subject = findSubjectBySchemaId(schemaId, topic, type).orElse(null);
      var properties = new HashMap<String, Object>();
      properties.put("schemaId", schemaId);
      properties.put("type", format.name());
      if (subject != null) {
        properties.put("subject", subject);
      }
      MessageFormatter formatter = schemaRegistryFormatters.get(format);
      return new DeserializeResult(
          formatter.format(topic, data),
          DeserializeResult.Type.JSON,
          properties
      );
    };
  }

  @SneakyThrows
  private Optional<String> findSubjectBySchemaId(int schemaId, String topic, Target type) {
    // Get subjects that could apply to this topic/type
    var applicableSubjects = getSchemaSubjects(topic, type);
    // Find the one that has this schemaId
    for (String subject : applicableSubjects) {
      var metadata = getSchemaBySubject(subject);
      if (metadata.isPresent() && metadata.get().getId() == schemaId) {
        return Optional.of(subject);
      }
    }
    return Optional.empty();
  }

  private SchemaType getMessageFormatBySchemaId(int schemaId) {
    return getSchemaById(schemaId)
        .map(ParsedSchema::schemaType)
        .flatMap(SchemaType::fromString)
        .orElseThrow(() -> new ValidationException(String.format("Schema for id '%d' not found ", schemaId)));
  }

  private int extractSchemaIdFromMsg(byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);
    if (buffer.remaining() >= SR_PAYLOAD_PREFIX_LENGTH && buffer.get() == SR_PAYLOAD_MAGIC_BYTE) {
      return buffer.getInt();
    }
    throw new ValidationException(
        String.format(
            "Data doesn't contain magic byte and schema id prefix, so it can't be deserialized with %s serde",
            NAME)
    );
  }
}
