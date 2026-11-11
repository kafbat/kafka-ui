package io.kafbat.ui.config;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nullable;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
  public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
                                 ResolvableType elementType, @Nullable MimeType mimeType,
                                 @Nullable Map<String, Object> hints) {
    Charset charset = mimeType != null ? mimeType.getCharset() : null;
    if (inputStream instanceof Mono || !String.class.equals(elementType.toClass()) || charset == null
        || isJacksonEncoding(charset) || getStreamingMediaTypeSeparator(mimeType) != null) {
      return super.encode(inputStream, bufferFactory, elementType, mimeType, hints);
    }
    // Jackson writes only Unicode encodings, so write ASCII-only JSON and encode it in the requested charset
    ObjectWriter writer = getObjectMapper().writer().with(JsonWriteFeature.ESCAPE_NON_ASCII);
    return Flux.from(inputStream).collectList().map(values -> {
      try {
        return bufferFactory.wrap(writer.writeValueAsString(values).getBytes(charset));
      } catch (JsonProcessingException e) {
        throw new EncodingException("JSON encoding error: " + e.getOriginalMessage(), e);
      }
    }).flux();
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

  private static boolean isJacksonEncoding(Charset charset) {
    return "US-ASCII".equals(charset.name())
        || Arrays.stream(JsonEncoding.values()).anyMatch(e -> e.getJavaName().equals(charset.name()));
  }

  private static boolean isJson(@Nullable MimeType mimeType) {
    return mimeType != null
        && (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mimeType) || mimeType.getSubtype().endsWith("+json"));
  }
}
