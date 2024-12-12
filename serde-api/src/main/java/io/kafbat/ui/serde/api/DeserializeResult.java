package io.kafbat.ui.serde.api;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Result of {@code Deserializer} work.
 *
 * @param result nullable
 */
public record DeserializeResult(String result, io.kafbat.ui.serde.api.DeserializeResult.Type type,
                                Map<String, Object> additionalProperties) {

  public enum Type {
    STRING, JSON
  }

  /**
   * @param result               string representation of deserialized binary data
   * @param type                 type of string - can it be converted to json or not
   * @param additionalProperties additional information about deserialized value (will be shown in UI)
   */
  public DeserializeResult(String result, Type type, Map<String, Object> additionalProperties) {
    this.result = result;
    this.type = type != null ? type : Type.STRING;
    this.additionalProperties = additionalProperties != null ? additionalProperties : Collections.emptyMap();
  }

  /**
   * @return string representation of deserialized binary data, can be null
   */
  @Override
  public String result() {
    return result;
  }

  /**
   * @return additional information about deserialized value.
   * Will be show as json dictionary in UI (serialized with Jackson object mapper).
   * It is recommended to use primitive types and strings for values.
   */
  @Override
  public Map<String, Object> additionalProperties() {
    return additionalProperties;
  }

  /**
   * @return type of deserialized result. Will be used as hint for some internal logic
   * (ex. if type==STRING smart filters won't try to parse it as json for further usage)
   */
  @Override
  public Type type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeserializeResult that = (DeserializeResult) o;
    return Objects.equals(result, that.result)
        && type == that.type
        && additionalProperties.equals(that.additionalProperties);
  }

  @Override
  public String toString() {
    return "DeserializeResult{"
        + "result='" + result
        + '\''
        + ", type=" + type
        + ", additionalProperties="
        + additionalProperties
        + '}';
  }
}
