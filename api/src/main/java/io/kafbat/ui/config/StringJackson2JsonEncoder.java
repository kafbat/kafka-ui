package io.kafbat.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeType;

// Writes Flux<String> under JSON media types as an array of strings instead of concatenating them.
public class StringJackson2JsonEncoder extends Jackson2JsonEncoder {

  public StringJackson2JsonEncoder(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
    if (String.class.equals(elementType.toClass()) && isJson(mimeType)) {
      return true;
    }
    return super.canEncode(elementType, mimeType);
  }

  @Override
  public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType,
                                @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
    if (value instanceof String text) {
      Charset charset = mimeType != null && mimeType.getCharset() != null
          ? mimeType.getCharset()
          : StandardCharsets.UTF_8;
      return bufferFactory.wrap(text.getBytes(charset));
    }
    return super.encodeValue(value, bufferFactory, valueType, mimeType, hints);
  }

  private static boolean isJson(@Nullable MimeType mimeType) {
    return mimeType != null
        && (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mimeType) || mimeType.getSubtype().endsWith("+json"));
  }
}
