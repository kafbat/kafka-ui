package io.kafbat.ui.serdes.builtin;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.RecordHeaders;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;

public class AvroEmbeddedSerde implements BuiltInSerde {
  public static final String NAME = "Avro (Embedded)";

  @Override
  public boolean canDeserialize(String topic, Target type) {
    return true;
  }

  @Override
  public Serializer serializer(String topic, Target type) {
    throw new IllegalStateException();
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return new Deserializer() {
      @SneakyThrows
      @Override
      public DeserializeResult deserialize(RecordHeaders headers, byte[] data) {
        try (var reader = new DataFileReader<>(new SeekableByteArrayInput(data), new GenericDatumReader<>())) {
          if (!reader.hasNext()) {
            // this is very strange situation, when only header present in payload
            // returning null in this case
            return new DeserializeResult(null, DeserializeResult.Type.JSON, Map.of());
          }
          Object avroObj = reader.next();
          String jsonValue = new String(AvroSchemaUtils.toJson(avroObj));
          return new DeserializeResult(jsonValue, DeserializeResult.Type.JSON, Map.of());
        }
      }
    };
  }
}
