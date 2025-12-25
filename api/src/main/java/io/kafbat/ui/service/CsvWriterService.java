package io.kafbat.ui.service;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategies;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.util.annotation.CsvIgnore;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CsvWriterService {
  private static final String INHERIT_COLUMN = "inherit";
  private static final Set<String> SKIPPED_COLUMNS = Set.of(INHERIT_COLUMN);

  private final ObjectMapper om;
  private final ClustersProperties.Csv properties;

  public CsvWriterService(ClustersProperties properties) {
    this.om = new ObjectMapper();
    this.om.setAnnotationIntrospector(
        AnnotationIntrospector.pair(
            new CustomIgnoreIntrospector(),
            new JacksonAnnotationIntrospector()
        )
    );
    this.properties = properties.getCsv();
  }

  private CsvWriter writer(StringWriter sw) {
    return CsvWriter.builder()
        .fieldSeparator(properties.getFieldSeparator())
        .quoteCharacter(properties.getQuoteCharacter())
        .quoteStrategy(QuoteStrategies.valueOf(properties.getQuoteStrategy().toUpperCase()))
        .lineDelimiter(LineDelimiter.valueOf(properties.getLineDelimeter().toUpperCase()))
        .build(sw);
  }

  public <T> Mono<String> write(Flux<T> items) {
    return items.collectList().map(this::write);
  }


  public <T> String write(List<T> items) {
    final StringWriter sw = new StringWriter();
    try (CsvWriter writer = writer(sw)) {
      if (!items.isEmpty()) {
        writer.writeRecord(mapHeader(items.getFirst()));
        for (T item : items) {
          writer.writeRecord(mapRecord(item));
        }
      }
    } catch (IOException ignored) {

    }
    return sw.toString();
  }

  private <T> List<String> mapHeader(T item) {
    JsonNode jsonNode = om.valueToTree(item);
    if (jsonNode.isObject()) {
      return jsonNode.properties().stream()
          .filter(p -> !SKIPPED_COLUMNS.contains(p.getKey()))
          .map(Map.Entry::getKey)
          .toList();
    } else {
      return List.of(jsonNode.asText());
    }
  }

  private <T> List<String> mapRecord(T item) {
    JsonNode jsonNode = om.valueToTree(item);
    if (jsonNode.isObject()) {
      return jsonNode.properties().stream()
          .filter(p -> !SKIPPED_COLUMNS.contains(p.getKey()))
          .map(Map.Entry::getValue)
          .map(this::toText)
          .toList();
    } else {
      return List.of(jsonNode.asText());
    }
  }

  private String toText(JsonNode e) {
    return switch (e) {
      case JsonNode j when j.isTextual() -> j.textValue();
      case JsonNode j when j.isNumber() || j.isBoolean() -> j.asText();
      case ArrayNode array -> Strings.join(array.elements(), ',');
      default -> e.toString();
    };
  }

  public class CustomIgnoreIntrospector extends JacksonAnnotationIntrospector {
    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
      return super.hasIgnoreMarker(m) || m.hasAnnotation(CsvIgnore.class);
    }
  }
}
