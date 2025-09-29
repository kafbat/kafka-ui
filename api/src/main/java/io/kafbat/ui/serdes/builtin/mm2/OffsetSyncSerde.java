package io.kafbat.ui.serdes.builtin.mm2;

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
public class OffsetSyncSerde extends MirrorMakerSerde implements BuiltInSerde {

  public static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("mm2-offset-syncs\\..*\\.internal");

  private static final Schema VALUE_SCHEMA;
  private static final Schema KEY_SCHEMA;

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
          case KEY -> deserializeKey(bytes);
          case VALUE -> deserializeValue(bytes);
        };
  }

  private static DeserializeResult deserializeKey(byte[] bytes) {
    Struct keyStruct = KEY_SCHEMA.read(ByteBuffer.wrap(bytes));

    return new DeserializeResult(toJson(keyStruct), DeserializeResult.Type.JSON, Map.of());
  }

  private static DeserializeResult deserializeValue(byte[] bytes) {
    Struct valueStruct = VALUE_SCHEMA.read(ByteBuffer.wrap(bytes));

    return new DeserializeResult(toJson(valueStruct), DeserializeResult.Type.JSON, Map.of());
  }
}
