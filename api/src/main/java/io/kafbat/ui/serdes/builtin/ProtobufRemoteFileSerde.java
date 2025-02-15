package io.kafbat.ui.serdes.builtin;

import static org.springframework.util.MultiValueMap.fromSingleValue;
import static org.springframework.util.ObjectUtils.isEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchemaUtils;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.RecordHeaders;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serdes.BuiltInSerde;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class ProtobufRemoteFileSerde implements BuiltInSerde {

  private HttpClient httpClient;
  private String path;
  private Map<String, String> queryParams;
  private ObjectMapper mapper;

  public static String name() {
    return "ProtobufRemoteFile";
  }

  @Override
  public void configure(PropertyResolver serdeProperties,
                        PropertyResolver kafkaClusterProperties,
                        PropertyResolver globalProperties) {
    configure(Configuration.create(serdeProperties));
  }

  @VisibleForTesting
  void configure(Configuration configuration) {
    if (configuration.httpClient() == null) {
      throw new ValidationException("Neither default, not per-topic descriptors defined for " + name() + " serde");
    }
    this.httpClient = configuration.httpClient();
    this.path = configuration.path();
    this.queryParams = configuration.queryParams();
    this.mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new JsonNullableModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.empty();
  }

  private Optional<Descriptor> getDescriptorFromRemote(String topic, Target type) {
    var params = new HashMap<>(queryParams);
    params.put("topic", topic);
    params.put("type", type.name());

    UriComponents uriComponents = UriComponentsBuilder.newInstance().queryParams(fromSingleValue(params)).build();

    var response = httpClient.get()
        .uri(path + "?" + uriComponents.getQuery())
        .responseSingle(((httpResponse, bytes) ->
            bytes.asString().map(this::read)
                .map(it -> new RemoteResponse(httpResponse.status(), it))
        ))
        .block();

    if (response == null || response.status() != HttpResponseStatus.OK || isEmpty(response.schema)) {
      throw new ValidationException(String.format("Error getting descriptor from remote for topic: %s", topic));
    }

    var messageTypeName = response.schema.msgTypeName;
    var resolvedSchema = response.schema.schema;

    return Optional.of(resolvedSchema)
        .map(ProtobufSchema::new)
        .map(it -> it.toDescriptor(messageTypeName));
  }

  @Override
  public boolean canDeserialize(String topic, Target type) {
    return getDescriptorFromRemote(topic, type).isPresent();
  }

  @Override
  public boolean canSerialize(String topic, Target type) {
    return getDescriptorFromRemote(topic, type).isPresent();
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    var descriptor = getDescriptorFromRemote(topic, type).orElseThrow();
    return new Deserializer() {
      @SneakyThrows
      @Override
      public DeserializeResult deserialize(RecordHeaders headers, byte[] data) {
        var protoMsg = DynamicMessage.parseFrom(descriptor, new ByteArrayInputStream(data));
        byte[] jsonFromProto = ProtobufSchemaUtils.toJson(protoMsg);
        var result = new String(jsonFromProto);
        return new DeserializeResult(
            result,
            DeserializeResult.Type.JSON,
            Map.of()
        );
      }
    };
  }

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Target type) {
    return Optional.empty();
  }

  private ResolvedSchema read(String response) {
    try {
      var parsedBody = mapper.readTree(response);

      var messageTypeName = parsedBody.get("msgTypeName").asText();
      var schema = parsedBody.get("schema").asText();
      return new ResolvedSchema(messageTypeName, schema);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  //----------------------------------------------------------------------------------------------------------------

  @VisibleForTesting
  record Configuration(
      HttpClient httpClient,
      String path,
      Map<String, String> queryParams
  ) {

    static Configuration create(PropertyResolver properties) {
      var url = properties.getProperty("url", String.class).orElseThrow();
      var path = properties.getProperty("path", String.class).orElseThrow();
      var timeout = properties.getProperty("timeout", String.class).map(Duration::parse).orElseThrow();

      Optional<Map<String, String>> queryParams = properties.getMapProperty("query_params", String.class, String.class);

      HttpClient httpClient = HttpClient
          .create()
          .proxyWithSystemProperties()
          .baseUrl(url)
          .responseTimeout(timeout);

      return new Configuration(httpClient, path, queryParams.orElse(Collections.emptyMap()));
    }
  }

  record RemoteResponse(
      HttpResponseStatus status,
      ResolvedSchema schema) {

  }

  record ResolvedSchema(
      String msgTypeName,
      String schema
  ) {

  }

}
