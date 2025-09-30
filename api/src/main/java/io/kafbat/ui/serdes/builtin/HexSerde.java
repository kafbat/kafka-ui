package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.HexFormat;
import java.util.Map;

public class HexSerde implements BuiltInSerde {
  public static final String NAME = "Hex";

  private HexFormat deserializeHexFormat;

  @Override
  public void autoConfigure(PropertyResolver kafkaClusterProperties, PropertyResolver globalProperties) {
    configure(" ", true);
  }

  @Override
  public void configure(PropertyResolver serdeProperties,
                        PropertyResolver kafkaClusterProperties,
                        PropertyResolver globalProperties) {
    String delim = serdeProperties.getProperty("delimiter", String.class).orElse(" ");
    boolean uppercase = serdeProperties.getProperty("uppercase", Boolean.class).orElse(true);
    configure(delim, uppercase);
  }

  private void configure(String delim, boolean uppercase) {
    deserializeHexFormat = HexFormat.ofDelimiter(delim);
    if (uppercase) {
      deserializeHexFormat = deserializeHexFormat.withUpperCase();
    }
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
    return input -> {
      input = input.trim();
      // it is a hack to provide ability to sent empty array as a key/value
      if (input.isEmpty()) {
        return new byte[] {};
      }
      return HexFormat.of().parseHex(prepareInputForParse(input));
    };
  }

  // removing most-common delimiters and prefixes
  private static String prepareInputForParse(String input) {
    return input
        .replaceAll(" ", "")
        .replaceAll("#", "")
        .replaceAll(":", "");
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return (headers, data) ->
        new DeserializeResult(
            deserializeHexFormat.formatHex(data),
            DeserializeResult.Type.STRING,
            Map.of()
        );
  }
}
