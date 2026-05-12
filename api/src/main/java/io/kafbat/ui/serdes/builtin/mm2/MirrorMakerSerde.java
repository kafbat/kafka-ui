package io.kafbat.ui.serdes.builtin.mm2;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serdes.BuiltInSerde;
import io.kafbat.ui.serdes.builtin.StructSerde;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;

@RequiredArgsConstructor
abstract class MirrorMakerSerde extends StructSerde implements BuiltInSerde {

  protected final boolean versioned;

  @Override
  public Deserializer deserializer(String topic, Target target) {
    return (recordHeaders, bytes) ->
        new DeserializeResult(toJson(switch (target) {
          case KEY -> deserializeKey(bytes);
          case VALUE -> deserializeValue(bytes);
        }),  DeserializeResult.Type.JSON, Map.of());
  }

  protected Struct deserializeKey(byte[] bytes) {
    return getKeySchema().read(ByteBuffer.wrap(bytes));
  }

  protected Struct deserializeValue(byte[] bytes) {
    ByteBuffer wrap = ByteBuffer.wrap(bytes);
    Optional<Schema> valueSchema;
    if (versioned) {
      short version = wrap.getShort();
      valueSchema = getVersionedValueSchema(version);
    } else {
      valueSchema = getValueSchema();
    }
    if (valueSchema.isPresent()) {
      return valueSchema.get().read(wrap);
    } else {
      throw new IllegalStateException("Value schema was not present");
    }
  }

  protected abstract Schema getKeySchema();

  protected Optional<Schema> getValueSchema() {
    return Optional.empty();
  }

  protected Optional<Schema> getVersionedValueSchema(short version) {
    throw new UnsupportedOperationException("Versioned value schema is not supported");
  }

}
