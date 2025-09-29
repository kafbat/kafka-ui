package io.kafbat.ui.serdes.builtin.mm2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.kafka.common.protocol.types.BoundField;
import org.apache.kafka.common.protocol.types.Struct;

abstract class MirrorMakerSerde {

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

  @SneakyThrows
  protected static String toJson(Struct s) {
    return JSON_MAPPER.writeValueAsString(s);
  }

}
