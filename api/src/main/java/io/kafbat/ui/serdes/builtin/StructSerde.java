package io.kafbat.ui.serdes.builtin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.kafka.common.protocol.types.BoundField;
import org.apache.kafka.common.protocol.types.Struct;

public abstract class StructSerde {

  private static final JsonMapper JSON_MAPPER = createMapper();

  private static JsonMapper createMapper() {
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

  @SneakyThrows
  protected String toJson(Struct s) {
    return JSON_MAPPER.writeValueAsString(s);
  }
}
