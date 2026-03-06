package io.kafbat.ui.serde.api;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.header.Headers;

/**
 * Main interface for serialization/deserialization logic.
 * It provides the ability to serialize, deserialize topic's keys and values, and optionally provides
 * information about data schema inside a topic.
 * <p>
 * <b>Lifecycle:</b><br/>
 * 1. on application startup kafbat-ui scans configs and finds all custom serde definitions<br/>
 * 2. for each custom serde its own separated child-first classloader is created<br/>
 * 3. kafbat-ui loads the class defined in configuration and instantiates an instance of that class using the default, non-arg constructor<br/>
 * 4. {@code configure(...)} method called<br/>
 * 5. various methods called during application runtime<br/>
 * 6. on application shutdown kafbat-ui calls {@code close()} method on serde instance<br/>
 * <p>
 * <b>Implementation considerations:</b><br/>
 * 1. Implementation class should have a default/non-arg constructor<br/>
 * 2. All methods except {@code configure(...)} and {@code close()} can be called from different threads. So, your code should be thread-safe.<br/>
 * 3. All methods will be executed in a separate child-first classloader.<br/>
 */
public interface Serde extends Closeable {

  /**
   * Kafka record's part that Serde will be applied to.
   */
  enum Target {
    /**
     * Should be used for key serialization/deserialization
     */
    KEY,
    /**
     * Should be used for value serialization/deserialization.
     */
    VALUE,
  }

  /**
   * Reads configuration using property resolvers and sets up the serde's internal state.
   *
   * @param serdeProperties        specific serde instance's properties
   * @param kafkaClusterProperties properties of the cluster for which serde is instantiated
   * @param globalProperties       global application properties
   */
  void configure(
      PropertyResolver serdeProperties,
      PropertyResolver kafkaClusterProperties,
      PropertyResolver globalProperties
  );

  /**
   * Get serde's description.
   * @return Serde's description. Treated as Markdown text. Will be shown in the UI.
   */
  Optional<String> getDescription();

  /**
   * Get schema description for the specified topic's key/value.
   * @param topic topic name
   * @param type  {@code Target} for which {@code SchemaDescription} will be returned.
   * @return SchemaDescription for the specified topic's key/value.
   * {@code Optional.empty} if there is no information about the schema.
   */
  Optional<SchemaDescription> getSchema(String topic, Target type);

  /**
   * Checks if this Serde can be applied to the specified topic's key/value deserialization.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Deserializer} will be applied.
   * @return true if this Serde can be applied to the specified topic's key/value deserialization
   */
  boolean canDeserialize(String topic, Target type);

  /**
   * Checks if this Serde can be applied to the specified topic's key/value serialization.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Serializer} will be applied.
   * @return true if this Serde can be applied to the specified topic's key/value serialization
   */
  boolean canSerialize(String topic, Target type);

  /**
   * Closes resources opened by Serde.
   */
  @Override
  default void close() {
    //intentionally left blank
  }

  //----------------------------------------------------------------------------

  /**
   * Creates {@code Serializer} for the specified topic's key/value.
   * kafbat-ui doesn't cache {@code Serializers} - a new one will be created each time a user's message needs to be serialized.
   * (Unless kafbat-ui supports batch inserts).
   * @param topic topic name
   * @param type  {@code Target} for which {@code Serializer} will be created.
   * @return {@code Serializer} for the specified topic's key/value.
   */
  Serializer serializer(String topic, Target type);

  /**
   * Creates {@code Serializer} for the specified topic's key/value, with additional properties
   * that may influence serialization (e.g. explicit schema subject).
   * Default implementation ignores properties and delegates to {@link #serializer(String, Target)}.
   *
   * @param topic      topic name
   * @param type       {@code Target} for which {@code Serializer} will be created.
   * @param properties additional serde-specific properties (e.g. {"subject": "my-subject"})
   * @return {@code Serializer} for the specified topic's key/value.
   */
  default Serializer serializer(String topic, Target type, Map<String, Object> properties) {
    return serializer(topic, type);
  }

  /**
   * Returns a list of subjects (or similar identifiers) that this serde can use
   * for the given topic and target. Used to populate UI dropdowns.
   * Default implementation returns an empty list.
   *
   * @param topic topic name
   * @param type  {@code Target} for which subjects will be returned.
   * @return list of applicable subjects, empty if not applicable.
   */
  default List<String> getSubjects(String topic, Target type) {
    return List.of();
  }

  /**
   * Creates {@code Deserializer} for the specified topic's key/value.
   * {@code Deserializer} will be created for each Kafka polling and will be used for all messages within that polling cycle.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Deserializer} will be created.
   * @return {@code Deserializer} for the specified topic's key/value.
   */
  Deserializer deserializer(String topic, Target type);

  /**
   * Serializes client's input to {@code byte[]} that will be sent to Kafka as key/value (depending on what {@code Type} it was created for).
   */
  interface Serializer {

    /**
     * Serializes input string to bytes.
     * @param input string entered by the user into the UI text field.<br/> Note: this input is not formatted in any way.
     * @return serialized bytes. Can be null if input is null or an empty string.
     */
    byte[] serialize(String input);

    /**
     * Serializes input string to bytes. Uses provided headers for additional information.
     * @param input string entered by the user into the UI text field.<br/> Note: this input is not formatted in any way.
     * @param headers headers entered by the user into the UI text field.<br/> Note: this input is not formatted in any way.
     * @return serialized bytes. Can be null if input is null or an empty string.
     */
    default byte[] serialize(String input, Headers headers) {
      return serialize(input);
    }
  }

  /**
   * Deserializes polled record's key/value (depending on what {@code Type} it was created for).
   */
  interface Deserializer {
    /**
     * Deserializes record's key/value to a string.
     * @param headers record's headers
     * @param data    record's key/value
     * @return deserialized object. Can be null if input is null or an empty string.
     */
    DeserializeResult deserialize(RecordHeaders headers, byte[] data);
  }

}
