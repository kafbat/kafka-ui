package io.kafbat.ui.serdes.builtin.mm2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.common.protocol.types.BoundField;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;

@RequiredArgsConstructor
abstract class MirrorMakerSerde implements BuiltInSerde {

  protected static final JsonMapper JSON_MAPPER = createMapper();

  protected static JsonMapper createMapper() {
    var module = new SimpleModule();
    module.addSerializer(Struct.class, new JsonSerializer<>() {
      @Override
      public void serialize(Struct value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        for (BoundField field : value.schema().fields()) {
          var fieldVal = value.get(field);
          gen.writeObjectField(field.def.name, fieldVal);
        }
        gen.writeEndObject();
      }
    });
    var mapper = new JsonMapper();
    mapper.registerModule(module);
    return mapper;
  }

  protected final boolean versioned;

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
    return false;
  }

  @Override
  public Serde.Serializer serializer(String topic, Serde.Target type) {
    throw new UnsupportedOperationException();
  }

  @SneakyThrows
  protected static String toJson(Struct s) {
    return JSON_MAPPER.writeValueAsString(s);
  }

  @Override
  public Deserializer deserializer(String topic, Target target) {
    return (recordHeaders, bytes) ->
        switch (target) {
          case KEY -> deserializeKey(bytes);
          case VALUE -> deserializeValue(bytes);
        };
  }

  protected DeserializeResult deserializeKey(byte[] bytes) {
    Struct keyStruct = getKeySchema().read(ByteBuffer.wrap(bytes));
    return new DeserializeResult(toJson(keyStruct), DeserializeResult.Type.JSON, Map.of());
  }

  protected DeserializeResult deserializeValue(byte[] bytes) {
    ByteBuffer wrap = ByteBuffer.wrap(bytes);
    Optional<Schema> valueSchema;
    if (versioned) {
      short version = wrap.getShort();
      valueSchema = getValueSchema(version);
    } else {
      valueSchema = getValueSchema();
    }
    if (valueSchema.isPresent()) {
      Struct valueStruct = valueSchema.get().read(wrap);
      return new DeserializeResult(toJson(valueStruct), DeserializeResult.Type.JSON, Map.of());
    } else {
      throw new IllegalStateException("Value schema was not present");
    }
  }

  protected abstract Schema getKeySchema();

  protected Optional<Schema> getValueSchema() {
    return Optional.empty();
  }

  protected Optional<Schema> getValueSchema(short version) {
    return Optional.empty();
  }

}
