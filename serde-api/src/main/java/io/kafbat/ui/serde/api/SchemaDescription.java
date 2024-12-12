package io.kafbat.ui.serde.api;

import java.util.Map;

/**
 * Description of topic's key/value schema.
 */
public record SchemaDescription(String schema, Map<String, Object> additionalProperties) {

  /**
   * @param schema               schema descriptions.
   *                             If contains json-schema (preferred) UI will use it for validation and sample data generation.
   * @param additionalProperties additional properties about schema (may be rendered in UI in the future)
   */
  public SchemaDescription {
  }

  /**
   * @return schema description text. Preferably contains json-schema. Can be null.
   */
  @Override
  public String schema() {
    return schema;
  }

  /**
   * @return additional properties about schema
   */
  @Override
  public Map<String, Object> additionalProperties() {
    return additionalProperties;
  }
}
