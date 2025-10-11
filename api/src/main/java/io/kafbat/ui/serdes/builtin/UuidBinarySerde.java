package io.kafbat.ui.serdes.builtin;

import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;


public class UuidBinarySerde implements BuiltInSerde {
  public static final String NAME = "UUIDBinary";

  private boolean mostSignificantBitsFirst = true;

  @Override
  public void configure(PropertyResolver serdeProperties,
                        PropertyResolver kafkaClusterProperties,
                        PropertyResolver globalProperties) {
    serdeProperties.getProperty("mostSignificantBitsFirst", Boolean.class)
        .ifPresent(msb -> UuidBinarySerde.this.mostSignificantBitsFirst = msb);
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
    return input -> {
      UUID uuid = UUID.fromString(input);
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      if (mostSignificantBitsFirst) {
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
      } else {
        bb.putLong(uuid.getLeastSignificantBits());
        bb.putLong(uuid.getMostSignificantBits());
      }
      return bb.array();
    };
  }

  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return (headers, data) -> {
      if (data.length != 16) {
        throw new ValidationException("UUID data should be 16 bytes, but it is " + data.length);
      }
      ByteBuffer bb = ByteBuffer.wrap(data);
      long msb = bb.getLong();
      long lsb = bb.getLong();
      UUID uuid = mostSignificantBitsFirst ? new UUID(msb, lsb) : new UUID(lsb, msb);
      return new DeserializeResult(
          uuid.toString(),
          DeserializeResult.Type.STRING,
          Map.of()
      );
    };
  }
}
