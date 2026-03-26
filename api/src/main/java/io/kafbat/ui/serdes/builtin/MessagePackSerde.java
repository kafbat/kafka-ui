package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.io.IOException;
import java.util.Map;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;


public class MessagePackSerde implements BuiltInSerde {
  public static final String NAME = "MessagePack";

  @Override
  public boolean canDeserialize(String topic, Serde.Target type) {
    return true;
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return (headers, data) -> {
      try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
        Value value = unpacker.unpackValue();
        return new DeserializeResult(value.toString(), DeserializeResult.Type.STRING, Map.of());
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to deserialize MessagePack payload", e);
      }
    };
  }
}
