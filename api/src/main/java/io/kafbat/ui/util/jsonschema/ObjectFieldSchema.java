package io.kafbat.ui.util.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

record ObjectFieldSchema(Map<String, FieldSchema> properties, List<String> required) implements FieldSchema {

  static final ObjectFieldSchema EMPTY = new ObjectFieldSchema(Map.of(), List.of());

  @Override
  public JsonNode toJsonNode(ObjectMapper mapper) {
    final Map<String, JsonNode> nodes = properties.entrySet().stream()
        .map(e -> Tuples.of(e.getKey(), e.getValue().toJsonNode(mapper)))
        .collect(Collectors.toMap(
            Tuple2::getT1,
            Tuple2::getT2
        ));
    final ObjectNode objectNode = mapper.createObjectNode();
    objectNode.setAll(
        new SimpleJsonType(JsonType.Type.OBJECT).toJsonNode(mapper)
    );
    objectNode.set("properties", mapper.valueToTree(nodes));
    if (!required.isEmpty()) {
      objectNode.set("required", mapper.valueToTree(required));
    }
    return objectNode;
  }
}
