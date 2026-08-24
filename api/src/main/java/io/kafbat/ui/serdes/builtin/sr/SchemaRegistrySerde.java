package io.kafbat.ui.serdes.builtin.sr;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeAvro;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeJson;
import static io.kafbat.ui.serdes.builtin.sr.Serialize.serializeProto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.schemaregistry.client.rest.entities.SubjectVersion;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaProvider;
import io.kafbat.ui.exception.UnknownSchemaTypeException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.SchemaRegistryDeserializePropertiesDTO;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.SerdeParameter;
import io.kafbat.ui.serdes.BuiltInSerde;
import io.kafbat.ui.service.ssl.SkipSecurityProvider;
import io.kafbat.ui.util.jsonschema.AvroJsonSchemaConverter;
import io.kafbat.ui.util.jsonschema.ProtobufSchemaConverter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import org.apache.kafka.common.config.SslConfigs;


public class SchemaRegistrySerde implements BuiltInSerde {
  private static final ObjectMapper OM = new ObjectMapper();

  public static final String NAME = "SchemaRegistry";
  public static final String SUBJECT_PARAMETER_NAME = "subject";
  public static final String MESSAGE_NAME_PARAMETER = "messageName";
  private static final byte SR_PAYLOAD_MAGIC_BYTE = 0x0;
  private static final int SR_PAYLOAD_PREFIX_LENGTH = 5;

  private static final String SCHEMA_REGISTRY = "schemaRegistry";
  private static final int DEFAULT_MAX_SUBJECTS_CACHE_SIZE = 1024;
  private static final int DEFAULT_ALL_SUBJECTS_CACHE_TTL_SECONDS = 30;

  private SchemaRegistryClient schemaRegistryClient;
  private List<String> schemaRegistryUrls;
  private String valueSchemaNameTemplate;
  private String keySchemaNameTemplate;
  private boolean checkSchemaExistenceForDeserialize;

  private Map<SchemaType, MessageFormatter> schemaRegistryFormatters;

  private Cache<Integer, List<String>> idToSubjectsCache;
  private Cache<String, Collection<String>> allSubjectsCache;

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
        formatterProperties,
        kafkaClusterProperties.getProperty("schemaRegistryMaxSubjectsCacheSize", Integer.class)
            .orElse(DEFAULT_MAX_SUBJECTS_CACHE_SIZE),
        kafkaClusterProperties.getProperty("schemaRegistryAllSubjectsCacheTtlSeconds", Integer.class)
            .orElse(DEFAULT_ALL_SUBJECTS_CACHE_TTL_SECONDS)
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
        formatterProperties,
        serdeProperties.getProperty("maxSubjectsCacheSize", Integer.class).orElse(DEFAULT_MAX_SUBJECTS_CACHE_SIZE),
        serdeProperties.getProperty("allSubjectsCacheTtlSeconds", Integer.class)
            .orElse(DEFAULT_ALL_SUBJECTS_CACHE_TTL_SECONDS)
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
        checkTopicSchemaExistenceForDeserialize, FormatterProperties.EMPTY, DEFAULT_MAX_SUBJECTS_CACHE_SIZE,
        DEFAULT_ALL_SUBJECTS_CACHE_TTL_SECONDS);
  }

  @VisibleForTesting
  void configure(
      List<String> schemaRegistryUrls,
      SchemaRegistryClient schemaRegistryClient,
      String keySchemaNameTemplate,
      String valueSchemaNameTemplate,
      boolean checkTopicSchemaExistenceForDeserialize,
      FormatterProperties formatterProperties,
      int maxSubjectsCacheSize,
      int allSubjectsCacheTtlSeconds) {
    this.schemaRegistryUrls = schemaRegistryUrls;
    this.schemaRegistryClient = schemaRegistryClient;
    this.keySchemaNameTemplate = keySchemaNameTemplate;
    this.valueSchemaNameTemplate = valueSchemaNameTemplate;
    this.schemaRegistryFormatters = MessageFormatter.createMap(schemaRegistryClient, formatterProperties);
    this.checkSchemaExistenceForDeserialize = checkTopicSchemaExistenceForDeserialize;
    this.idToSubjectsCache = Caffeine.newBuilder()
        .maximumSize(maxSubjectsCacheSize)
        .build();
    this.allSubjectsCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(allSubjectsCacheTtlSeconds))
        .maximumSize(1)
        .build();
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
        .orElseThrow(() -> new UnknownSchemaTypeException(schema.getSchemaType()));
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
  List<String> getSchemaSubjects(String topic, Target type) {
    var allSubjects = allSubjectsCache.get("all", k -> {
      try {
        return schemaRegistryClient.getAllSubjects();
      } catch (Exception e) {
        throw new RuntimeException("Error fetching all subjects from Schema Registry", e);
      }
    });
    if (allSubjects == null || allSubjects.isEmpty()) {
      return List.of();
    }

    String defaultSubject = schemaSubject(topic, type);
    String topicPrefix = topic + "-";
    // Exclude subjects for the opposite type
    String excludeSuffix = type == Target.KEY ? "-value" : "-key";

    return allSubjects.stream()
        .filter(subject -> {
          if (subject.endsWith(excludeSuffix)) {
            return false;
          }

          boolean isDefaultSubject = subject.equals(defaultSubject);
          boolean isTopicName = subject.startsWith(topicPrefix);
          boolean isNotKeyOrValue =
              !subject.endsWith("-key") && !subject.endsWith("-value");

          return isDefaultSubject || isTopicName || isNotKeyOrValue;
        })
        .toList();
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    return buildSerializer(topic, type, schemaSubject(topic, type), null);
  }

  @Override
  public Serializer serializer(String topic, Target type, Map<String, Object> properties) {
    String subject = schemaSubject(topic, type);
    String messageName = null;
    if (properties != null) {
      Object subjectObj = properties.get(SUBJECT_PARAMETER_NAME);
      if (subjectObj instanceof String explicitSubject && !explicitSubject.isEmpty()) {
        subject = explicitSubject;
      }
      Object messageNameObj = properties.get(MESSAGE_NAME_PARAMETER);
      if (messageNameObj instanceof String explicitMessageName && !explicitMessageName.isBlank()) {
        messageName = explicitMessageName;
      }
    }
    return buildSerializer(topic, type, subject, messageName);
  }

  @Override
  public List<SerdeParameter> getParameters(String topic, Target type) {
    List<SerdeParameter> parameters = new ArrayList<>();
    parameters.add(
        new SerdeParameter(SUBJECT_PARAMETER_NAME, SUBJECT_PARAMETER_NAME, getSchemaSubjects(topic, type)));
    // for protobuf schemas with multiple messages, let the user pick which message to produce
    List<String> messageNames = getProtobufMessageNames(schemaSubject(topic, type));
    if (!messageNames.isEmpty()) {
      parameters.add(new SerdeParameter(MESSAGE_NAME_PARAMETER, MESSAGE_NAME_PARAMETER, messageNames));
    }
    return parameters;
  }

  @Override
  public boolean couldBePreferable(String topic, Target type) {
    return getSchemaSubjects(topic, type).contains(schemaSubject(topic, type));
  }

  private Serializer buildSerializer(String topic, Target type, String subject, @Nullable String messageName) {
    SchemaMetadata meta = getSchemaBySubject(subject)
        .orElseThrow(() -> new ValidationException(
            String.format("No schema for subject '%s' found", subject)));
    ParsedSchema schema = getSchemaById(meta.getId())
        .orElseThrow(() -> new IllegalStateException(
            String.format("Schema not found for id %s, subject '%s'", meta.getId(), subject)));
    SchemaType schemaType = SchemaType.fromString(meta.getSchemaType())
        .orElseThrow(() -> new UnknownSchemaTypeException(meta.getSchemaType()));
    return switch (schemaType) {
      case PROTOBUF -> input ->
          serializeProto(schemaRegistryClient, topic, type, (ProtobufSchema) schema, meta.getId(),
              messageName, input);
      case AVRO -> input ->
          serializeAvro((AvroSchema) schema, meta.getId(), input);
      case JSON -> input ->
          serializeJson((JsonSchema) schema, meta.getId(), input);
    };
  }

  // Returns all message type names for a protobuf subject (empty for non-protobuf or missing subjects).
  private List<String> getProtobufMessageNames(String subject) {
    try {
      var metaOpt = getSchemaBySubject(subject);
      if (metaOpt.isEmpty()
          || SchemaType.fromString(metaOpt.get().getSchemaType()).orElse(null) != SchemaType.PROTOBUF) {
        return List.of();
      }
      return getSchemaById(metaOpt.get().getId())
          .filter(ProtobufSchema.class::isInstance)
          .map(schema -> collectProtobufMessageNames((ProtobufSchema) schema))
          .orElseGet(List::of);
    } catch (Exception e) {
      return List.of();
    }
  }

  private static List<String> collectProtobufMessageNames(ProtobufSchema schema) {
    Descriptors.Descriptor first = schema.toDescriptor();
    if (first == null) {
      return List.of();
    }
    List<String> names = new ArrayList<>();
    collectProtobufMessages(first.getFile().getMessageTypes(), names);
    return names.stream().distinct().sorted().toList();
  }

  private static void collectProtobufMessages(List<Descriptors.Descriptor> descriptors, List<String> acc) {
    for (Descriptors.Descriptor descriptor : descriptors) {
      // skip synthetic map-entry types - they aren't real, producible message definitions
      if (descriptor.getOptions().getMapEntry()) {
        continue;
      }
      acc.add(descriptor.getFullName());
      collectProtobufMessages(descriptor.getNestedTypes(), acc);
    }
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return (headers, data) -> {
      var schemaId = extractSchemaIdFromMsg(data);
      ParsedSchema schema = getSchemaById(schemaId)
              .orElseThrow(() -> new ValidationException(String.format("Schema not found %s", schemaId)));
      List<String> subjects = getSubjectsById(schemaId);
      SchemaType format = getMessageFormatBySchemaId(schema);

      var properties = new SchemaRegistryDeserializePropertiesDTO();
      properties.setId(schemaId);
      properties.setSubjects(subjects);
      properties.setType(format.name());

      MessageFormatter formatter = schemaRegistryFormatters.get(format);

      return new DeserializeResult(
          formatter.format(topic, data),
          DeserializeResult.Type.JSON,
          OM.convertValue(properties, new TypeReference<>() {
          })
      );
    };
  }

  private List<String> getSubjectsById(int schemaId) {
    return idToSubjectsCache.get(schemaId, (id) -> {
      try {
        return schemaRegistryClient.getAllVersionsById(id).stream()
            .map(SubjectVersion::getSubject)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private SchemaType getMessageFormatBySchemaId(ParsedSchema schema) {
    return SchemaType.fromString(schema.schemaType())
        .orElseThrow(() -> new ValidationException(String.format("Schema type not found %s", schema.schemaType())));
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
