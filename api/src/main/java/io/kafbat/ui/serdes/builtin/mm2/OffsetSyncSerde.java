package io.kafbat.ui.serdes.builtin.mm2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;

@Slf4j
public class OffsetSyncSerde implements BuiltInSerde {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final String TOPIC_KEY = "topic";
  public static final String PARTITION_KEY = "partition";
  public static final String UPSTREAM_OFFSET_KEY = "upstreamOffset";
  public static final String DOWNSTREAM_OFFSET_KEY = "offset";
  public static final Schema VALUE_SCHEMA;
  public static final Schema KEY_SCHEMA;

  static {
    VALUE_SCHEMA = new Schema(new Field("upstreamOffset", Type.INT64), new Field("offset", Type.INT64));
    KEY_SCHEMA = new Schema(new Field("topic", Type.STRING), new Field("partition", Type.INT32));
  }

  public static String name() {
    return "OffsetSync";
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

          case KEY: {
            Struct keyStruct = KEY_SCHEMA.read(ByteBuffer.wrap(bytes));
            String t = keyStruct.getString(TOPIC_KEY);
            int partition = keyStruct.getInt(PARTITION_KEY);

            var map = Map.of(
                TOPIC_KEY, t,
                PARTITION_KEY, partition
            );

            try {
              var result = OBJECT_MAPPER.writeValueAsString(map);
              yield new DeserializeResult(result, DeserializeResult.Type.JSON, Map.of());
            } catch (JsonProcessingException e) {
              log.error("Error deserializing record", e);
              throw new RuntimeException("Error deserializing record", e);
            }
          }

          case VALUE: {
            Struct valueStruct = VALUE_SCHEMA.read(ByteBuffer.wrap(bytes));
            var map = Map.of(
                UPSTREAM_OFFSET_KEY, valueStruct.getLong(UPSTREAM_OFFSET_KEY),
                DOWNSTREAM_OFFSET_KEY, valueStruct.getLong(DOWNSTREAM_OFFSET_KEY)
            );

            try {
              var result = OBJECT_MAPPER.writeValueAsString(map);
              yield new DeserializeResult(result, DeserializeResult.Type.JSON, Map.of());
            } catch (JsonProcessingException e) {
              log.error("Error deserializing record", e);
              throw new RuntimeException("Error deserializing record", e);
            }
          }

        };
  }
}
