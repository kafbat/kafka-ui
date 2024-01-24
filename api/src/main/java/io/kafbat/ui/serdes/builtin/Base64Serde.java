package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class Base64Serde implements BuiltInSerde {

  public static String name() {
    return "Base64";
  }

  @Override
  public Optional<String> getDescription() {
    return Optional.empty();
  }

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Serde.Target type) {
    return Optional.empty();
  }

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
      if (inputString.length() == 0) {
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
