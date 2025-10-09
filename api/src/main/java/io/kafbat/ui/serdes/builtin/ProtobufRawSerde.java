package io.kafbat.ui.serdes.builtin;

import com.google.protobuf.UnknownFieldSet;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.RecordHeaders;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;

public class ProtobufRawSerde implements BuiltInSerde {
  public static final String NAME = "ProtobufDecodeRaw";

  @Override
  public boolean canDeserialize(String topic, Serde.Target type) {
    return true;
  }


  @Override
  public Serde.Deserializer deserializer(String topic, Serde.Target type) {
    return new Serde.Deserializer() {
        @SneakyThrows
        @Override
        public DeserializeResult deserialize(RecordHeaders headers, byte[] data) {
            try {
              UnknownFieldSet unknownFields = UnknownFieldSet.parseFrom(data);
              return new DeserializeResult(unknownFields.toString(), DeserializeResult.Type.STRING, Map.of());
            } catch (Exception e) {
              throw new ValidationException(e.getMessage());
            }
        }
    };
  }
}
