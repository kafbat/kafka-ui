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
public class CheckpointSerde implements BuiltInSerde {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final String TOPIC_KEY = "topic";
  public static final String PARTITION_KEY = "partition";
  public static final String CONSUMER_GROUP_ID_KEY = "group";
  public static final String UPSTREAM_OFFSET_KEY = "upstreamOffset";
  public static final String DOWNSTREAM_OFFSET_KEY = "offset";
  public static final String METADATA_KEY = "metadata";
  public static final String VERSION_KEY = "version";
  public static final short VERSION = 0;

  public static final Schema VALUE_SCHEMA_V0 = new Schema(
      new Field(UPSTREAM_OFFSET_KEY, Type.INT64),
      new Field(DOWNSTREAM_OFFSET_KEY, Type.INT64),
      new Field(METADATA_KEY, Type.STRING));

  public static final Schema KEY_SCHEMA = new Schema(
      new Field(CONSUMER_GROUP_ID_KEY, Type.STRING),
      new Field(TOPIC_KEY, Type.STRING),
      new Field(PARTITION_KEY, Type.INT32));

  public static final Schema HEADER_SCHEMA = new Schema(
      new Field(VERSION_KEY, Type.INT16));

  public static String name() {
    return "Checkpoint";
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

            String group = keyStruct.getString(CONSUMER_GROUP_ID_KEY);
            String t = keyStruct.getString(TOPIC_KEY);
            int partition = keyStruct.getInt(PARTITION_KEY);

            var map = Map.of(
                CONSUMER_GROUP_ID_KEY, group,
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
            ByteBuffer value = ByteBuffer.wrap(bytes);
            Struct header = HEADER_SCHEMA.read(value);
            short version = header.getShort(VERSION_KEY);
            Schema valueSchema = valueSchema(version);
            Struct valueStruct = valueSchema.read(value);

            long upstreamOffset = valueStruct.getLong(UPSTREAM_OFFSET_KEY);
            long downstreamOffset = valueStruct.getLong(DOWNSTREAM_OFFSET_KEY);
            String metadata = valueStruct.getString(METADATA_KEY);

            var map = Map.of(
                UPSTREAM_OFFSET_KEY, upstreamOffset,
                DOWNSTREAM_OFFSET_KEY, downstreamOffset,
                METADATA_KEY, metadata
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

  private static Schema valueSchema(short version) {
    assert version == 0;
    return VALUE_SCHEMA_V0;
  }

}
