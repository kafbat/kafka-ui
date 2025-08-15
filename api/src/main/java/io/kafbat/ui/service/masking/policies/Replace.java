package io.kafbat.ui.service.masking.policies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Preconditions;
import java.util.Map;

class Replace extends MaskingPolicy {

  static final String DEFAULT_REPLACEMENT = "***DATA_MASKED***";

  private final String replacement;

  Replace(FieldsSelector fieldsSelector, String replacementString) {
    super(fieldsSelector);
    this.replacement = Preconditions.checkNotNull(replacementString);
  }

  @Override
  public String applyToString(String str) {
    return replacement;
  }

  @Override
  public ContainerNode<?> applyToJsonContainer(ContainerNode<?> node) {
    return (ContainerNode<?>) replaceWithFieldsCheck(node);
  }

  private JsonNode replaceWithFieldsCheck(JsonNode node) {
    if (node.isObject()) {
      ObjectNode obj = ((ObjectNode) node).objectNode();
      for (Map.Entry<String, JsonNode> property : node.properties()) {
        String fieldName = property.getKey();
        JsonNode fieldVal = property.getValue();
        if (fieldShouldBeMasked(fieldName)) {
          obj.set(fieldName, replaceRecursive(fieldVal));
        } else {
          obj.set(fieldName, replaceWithFieldsCheck(fieldVal));
        }
      }
      return obj;
    } else if (node.isArray()) {
      ArrayNode arr = ((ArrayNode) node).arrayNode(node.size());
      node.elements().forEachRemaining(e -> arr.add(replaceWithFieldsCheck(e)));
      return arr;
    }
    // if it is not an object or array - we have nothing to replace here
    return node;
  }

  private JsonNode replaceRecursive(JsonNode node) {
    if (node.isObject()) {
      ObjectNode obj = ((ObjectNode) node).objectNode();
      for (Map.Entry<String, JsonNode> property : node.properties()) {
        obj.set(property.getKey(), replaceRecursive(property.getValue()));
      }
      return obj;
    } else if (node.isArray()) {
      ArrayNode arr = ((ArrayNode) node).arrayNode(node.size());
      node.elements().forEachRemaining(e -> arr.add(replaceRecursive(e)));
      return arr;
    }
    return new TextNode(replacement);
  }
}
