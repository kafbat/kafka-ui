package io.kafbat.ui.serde.api;

import java.util.List;

/**
 * Describes a SerDe (Serializer/Deserializer) parameter and how its value
 * should be interpreted during serialization/deserialization.
 * <p>
 * A parameter is identified by its {@link #name} and may optionally define
 * a set of {@link #allowedValues} that constrain or guide how an input string
 * should be resolved to a concrete value.
 */
public class SerdeParameter {

  /**
   * Internal parameter name.
   * <p>
   * This is the key used when passing parameter values to the SerDe layer.
   */
  private final String name;

  /**
   * Human-readable name of the parameter.
   * <p>
   * Useful for logs, debugging, or external representations.
   */
  private final String visibleName;

  /**
   * Optional list of allowed values that can be used to resolve the parameter.
   * <p>
   * For example, for a parameter with {@code name = "subject"}, this list may
   * contain available Kafka Schema subjects:
   * <pre>
   * ["orders-value", "payments-value", "users-value"]
   * </pre>
   * During deserialization, an input string is expected to match one of these
   * values in order to be considered valid or to resolve to the correct subject.
   * <p>
   * If empty, the parameter accepts arbitrary values and no predefined lookup
   * or validation is applied.
   */
  private final List<String> allowedValues;

  public SerdeParameter(String name) {
    this(name, name);
  }

  public SerdeParameter(String name, String visibleName) {
    this(name, visibleName, List.of());
  }

  public SerdeParameter(String name, String visibleName, List<String> allowedValues) {
    this.name = name;
    this.visibleName = visibleName;
    this.allowedValues = allowedValues;
  }

  public String getName() {
    return name;
  }

  public String getVisibleName() {
    return visibleName;
  }

  public List<String> getAllowedValues() {
    return allowedValues;
  }
}
