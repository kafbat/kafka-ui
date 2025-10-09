package io.kafbat.ui.serdes.builtin;

import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedInteger;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Map;
import java.util.Optional;

public class UInt32Serde implements BuiltInSerde {
  public static final String NAME = "UInt32";

  @Override
  public Optional<SchemaDescription> getSchema(String topic, Serde.Target type) {
    return Optional.of(
        new SchemaDescription(
            String.format(
                "{ "
                    + "  \"type\" : \"integer\", "
                    + "  \"minimum\" : 0, "
                    + "  \"maximum\" : %s"
                    + "}",
                UnsignedInteger.MAX_VALUE
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
    return input -> Ints.toByteArray(Integer.parseUnsignedInt(input));
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return (headers, data) ->
        new DeserializeResult(
            UnsignedInteger.fromIntBits(Ints.fromByteArray(data)).toString(),
            DeserializeResult.Type.JSON,
            Map.of()
        );
  }
}
