package io.kafbat.ui.serdes.builtin;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.RecordHeaders;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;

public class AvroEmbeddedSerde implements BuiltInSerde {
  public static final String NAME = "Avro (Embedded)";

  // Iceberg AvroEncoderUtil uses C2 01 + DataOutputStream.writeUTF(schema) + Avro datum.
  // This is distinct from both Avro object containers and single-object fingerprint encoding.
  private DeserializeResult deserializeIceberg(byte[] data) throws IOException {
    try (var input = new DataInputStream(new ByteArrayInputStream(data, 2, data.length - 2))) {
      Schema schema = new Schema.Parser().parse(input.readUTF());
      var decoder = DecoderFactory.get().binaryDecoder(input, null);
      Object datum = new GenericDatumReader<>(schema).read(null, decoder);
      if (!decoder.isEnd()) {
        throw new IOException("Trailing bytes after Iceberg embedded Avro datum");
      }
      String json = datum == null ? "null" : new String(AvroSchemaUtils.toJson(datum), StandardCharsets.UTF_8);
      return new DeserializeResult(json,
          DeserializeResult.Type.JSON, Map.of());
    }
  }

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
        if (data.length >= 2 && data[0] == (byte) 0xC2 && data[1] == 0x01) {
          return deserializeIceberg(data);
        }
        try (var reader = new DataFileReader<>(new SeekableByteArrayInput(data), new GenericDatumReader<>())) {
          if (!reader.hasNext()) {
            // this is very strange situation, when only header present in payload
            // returning null in this case
            return new DeserializeResult(null, DeserializeResult.Type.JSON, Map.of());
          }
          Object avroObj = reader.next();
          String jsonValue = new String(AvroSchemaUtils.toJson(avroObj), StandardCharsets.UTF_8);
          return new DeserializeResult(jsonValue, DeserializeResult.Type.JSON, Map.of());
        }
      }
    };
  }
}
