package io.kafbat.ui.serdes.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.RecordHeaders;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;

public class AvroEmbeddedSerde implements BuiltInSerde {
  private static final JsonMapper JSON = new JsonMapper();
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
      json = renderLogicalTypes(schema, JSON.readTree(json)).toString();
      return new DeserializeResult(json,
          DeserializeResult.Type.JSON, Map.of());
    }
  }

  // Avro JSON represents fixed values as byte strings, even for logical UUIDs.
  // Use the writer schema so arbitrary binary fields are never mistaken for UUIDs.
  private JsonNode renderLogicalTypes(Schema schema, JsonNode value) {
    if (value.isNull()) {
      return value;
    }
    switch (schema.getType()) {
      case FIXED:
        if (schema.getFixedSize() == 16 && "uuid".equals(schema.getProp("logicalType"))) {
          var bytes = ByteBuffer.wrap(value.textValue().getBytes(StandardCharsets.ISO_8859_1));
          return TextNode.valueOf(new UUID(bytes.getLong(), bytes.getLong()).toString());
        }
        break;
      case RECORD:
        for (var field : schema.getFields()) {
          ((ObjectNode) value).set(field.name(), renderLogicalTypes(field.schema(), value.get(field.name())));
        }
        break;
      case ARRAY:
        for (int i = 0; i < value.size(); i++) {
          ((ArrayNode) value).set(i, renderLogicalTypes(schema.getElementType(), value.get(i)));
        }
        break;
      case MAP:
        value.properties().forEach(entry ->
            ((ObjectNode) value).set(entry.getKey(), renderLogicalTypes(schema.getValueType(), entry.getValue())));
        break;
      case UNION:
        // Avro JSON wraps a non-null union branch using its type's full name.
        for (var branch : schema.getTypes()) {
          if (value.has(branch.getFullName())) {
            ((ObjectNode) value).set(branch.getFullName(),
                renderLogicalTypes(branch, value.get(branch.getFullName())));
            break;
          }
        }
        break;
      default:
        break;
    }
    return value;
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
