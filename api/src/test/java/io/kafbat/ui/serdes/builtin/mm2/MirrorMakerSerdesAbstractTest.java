package io.kafbat.ui.serdes.builtin.mm2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.serdes.RecordHeadersImpl;
import java.util.Base64;
import java.util.Map;

public abstract class MirrorMakerSerdesAbstractTest {

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  protected static final String TOPIC = "test-topic";
  protected static final RecordHeadersImpl HEADERS = new RecordHeadersImpl();

  protected Map<String, Object> jsonToMap(String json) throws JsonProcessingException {
    //@formatter:off
    return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    //@formatter:on
  }

  protected static byte[] decodeBase64(String base64) {
    return Base64.getDecoder().decode(base64.trim());
  }

}
