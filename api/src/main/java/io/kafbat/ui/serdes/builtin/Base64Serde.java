package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Base64;
import java.util.Map;

public class Base64Serde implements BuiltInSerde {
  public static final String NAME = "Base64";

  @Override
  public boolean canDeserialize(String topic, Serde.Target type) {
    return true;
  }

  @Override
  public boolean canSerialize(String topic, Serde.Target type) {
    return true;
  }

  @Override
  public Serde.Serializer serializer(String topic, Serde.Target type) {
    var decoder = Base64.getDecoder();
    return inputString -> {
      inputString = inputString.trim();
      // it is actually a hack to provide ability to sent empty array as a key/value
      if (inputString.isEmpty()) {
        return new byte[] {};
      }
      return decoder.decode(inputString);
    };
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    var encoder = Base64.getEncoder();
    return (headers, data) ->
        new DeserializeResult(
            encoder.encodeToString(data),
            DeserializeResult.Type.STRING,
            Map.of()
        );
  }
}
