package io.kafbat.ui.serdes.builtin.mm2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;

@Slf4j
public class HeartbeatSerde implements BuiltInSerde {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String SOURCE_CLUSTER_ALIAS_KEY = "sourceClusterAlias";
  private static final String TARGET_CLUSTER_ALIAS_KEY = "targetClusterAlias";
  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String VERSION_KEY = "version";

  private static final Schema VALUE_SCHEMA_V0 = new Schema(
      new Field(TIMESTAMP_KEY, Type.INT64));

  private static final Schema KEY_SCHEMA = new Schema(
      new Field(SOURCE_CLUSTER_ALIAS_KEY, Type.STRING),
      new Field(TARGET_CLUSTER_ALIAS_KEY, Type.STRING));

  private static final Schema HEADER_SCHEMA = new Schema(
      new Field(VERSION_KEY, Type.INT16));

  public static String name() {
    return "Heartbeat";
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.empty();
  }

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Target type) {
    return Optional.empty();
  }

  @Override
  public boolean canDeserialize(String topic, Target type) {
    return true;
  }

  @Override
  public boolean canSerialize(String topic, Target type) {
    return false;
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Deserializer deserializer(String topic, Target target) {
    return (recordHeaders, bytes) ->
        switch (target) {
          case KEY -> deserializeKey(bytes);
          case VALUE -> deserializeValue(bytes);
        };
  }

  private static DeserializeResult deserializeKey(byte[] bytes) {
    Struct keyStruct = KEY_SCHEMA.read(ByteBuffer.wrap(bytes));
    String sourceClusterAlias = keyStruct.getString(SOURCE_CLUSTER_ALIAS_KEY);
    String targetClusterAlias = keyStruct.getString(TARGET_CLUSTER_ALIAS_KEY);

    var map = Map.of(
        "sourceClusterAlias", sourceClusterAlias,
        "targetClusterAlias", targetClusterAlias
    );

    try {
      var result = OBJECT_MAPPER.writeValueAsString(map);
      return new DeserializeResult(result, DeserializeResult.Type.JSON, Map.of());
    } catch (JsonProcessingException e) {
      log.error("Error deserializing record", e);
      throw new RuntimeException("Error deserializing record", e);
    }
  }

  private static DeserializeResult deserializeValue(byte[] bytes) {
    ByteBuffer value = ByteBuffer.wrap(bytes);
    Struct headerStruct = HEADER_SCHEMA.read(value);
    short version = headerStruct.getShort(VERSION_KEY);
    Struct valueStruct = valueSchema(version).read(value);
    long timestamp = valueStruct.getLong(TIMESTAMP_KEY);
    return new DeserializeResult(String.valueOf(timestamp), DeserializeResult.Type.STRING, Map.of());
  }

  private static Schema valueSchema(short version) {
    assert version == 0;
    return VALUE_SCHEMA_V0;
  }
}
