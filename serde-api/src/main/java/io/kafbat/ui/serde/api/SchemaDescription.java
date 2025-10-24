package io.kafbat.ui.serde.api;

import java.util.Map;

/**
 * Description of topic's key/value schema.
 */
public final class SchemaDescription {

  private final String schema;
  private final Map<String, Object> additionalProperties;

  /**
   * Constructor for {@code SchemaDescription}.
   * @param schema schema description.
   *              If it contains a JSON schema (preferred), the UI will use it for validation and sample data generation.
   * @param additionalProperties additional properties about the schema (may be rendered in the UI in the future)
   */
  public SchemaDescription(String schema, Map<String, Object> additionalProperties) {
    this.schema = schema;
    this.additionalProperties = additionalProperties;
  }

  /**
   * Schema description text. Can be null.
   * @return schema description text. Preferably contains a JSON schema. Can be null.
   */
  public String getSchema() {
    return schema;
  }

  /**
   * Additional properties about the schema.
   * @return additional properties about the schema
   */
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }
}
