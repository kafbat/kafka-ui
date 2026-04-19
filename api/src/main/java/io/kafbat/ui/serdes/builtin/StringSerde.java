package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StringSerde implements BuiltInSerde {
  public static final String NAME = "String";

  private Charset encoding = StandardCharsets.UTF_8;

  @Override
  public void configure(PropertyResolver serdeProperties,
                        PropertyResolver kafkaClusterProperties,
                        PropertyResolver globalProperties) {
    serdeProperties.getProperty("encoding", String.class)
        .map(Charset::forName)
        .ifPresent(e -> StringSerde.this.encoding = e);
  }

  @Override
  public boolean canDeserialize(String topic, Target type) {
    return true;
  }

  @Override
  public boolean canSerialize(String topic, Target type) {
    return true;
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    return input -> input.getBytes(encoding);
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return (headers, data) ->
        new DeserializeResult(
            new String(data, encoding),
            DeserializeResult.Type.STRING,
            Map.of()
        );
  }

}
