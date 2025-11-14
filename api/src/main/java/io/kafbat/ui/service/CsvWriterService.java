package io.kafbat.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategies;
import io.kafbat.ui.config.ClustersProperties;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CsvWriterService {
  private final ObjectMapper om;
  private final ClustersProperties.Csv properties;

  public CsvWriterService(ObjectMapper om,
                          ClustersProperties properties) {
    this.om = om;
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
    final CsvWriter writer = writer(sw);

    if (!items.isEmpty()) {
      writer.writeRecord(mapHeader(items.getFirst()));
      for (T item : items) {
        writer.writeRecord(mapRecord(item));
      }
    }
    return sw.toString();
  }

  private <T> List<String> mapHeader(T item) {
    JsonNode jsonNode = om.valueToTree(item);
    if (jsonNode.isObject()) {
      return jsonNode.properties().stream()
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
          .map(Map.Entry::getValue)
          .map(JsonNode::asText)
          .toList();
    } else {
      return List.of(jsonNode.asText());
    }
  }
}
