package io.kafbat.ui.serdes.builtin;

import com.google.common.primitives.Longs;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Map;
import java.util.Optional;

public class Int64Serde implements BuiltInSerde {
  public static final String NAME = "Int64";

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Serde.Target type) {
    return Optional.of(
        new SchemaDescription(
            String.format(
                "{ "
                    + "  \"type\" : \"integer\", "
                    + "  \"minimum\" : %s, "
                    + "  \"maximum\" : %s "
                    + "}",
                Long.MIN_VALUE,
                Long.MAX_VALUE
            ),
            Map.of()
        )
    );
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
    return input -> Longs.toByteArray(Long.parseLong(input));
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return (headers, data) ->
        new DeserializeResult(
            String.valueOf(Longs.fromByteArray(data)),
            DeserializeResult.Type.JSON,
            Map.of()
        );
  }
}
