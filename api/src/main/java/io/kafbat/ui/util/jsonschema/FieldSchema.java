package io.kafbat.ui.util.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

interface FieldSchema {
  JsonNode toJsonNode(ObjectMapper mapper);
}
