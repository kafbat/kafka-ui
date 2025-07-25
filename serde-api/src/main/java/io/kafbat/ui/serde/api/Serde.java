package io.kafbat.ui.serde.api;

import java.io.Closeable;
import java.util.Optional;
import org.apache.kafka.common.header.Headers;

/**
 * Main interface of  serialization/deserialization logic.
 * It provides ability to serialize, deserialize topic's keys and values, and optionally provides
 * information about data schema inside topic.
 * <p>
 * <b>Lifecycle:</b><br/>
 * 1. on application startup kafbat-ui scans configs and finds all custom serde definitions<br/>
 * 2. for each custom serde its own separated child-first classloader is created<br/>
 * 3. kafbat-ui loads class defined in configuration and instantiates instance of that class using default, non-arg constructor<br/>
 * 4. {@code configure(...)} method called<br/>
 * 5. various methods called during application runtime<br/>
 * 6. on application shutdown kafbat-ui calls {@code close()} method on serde instance<br/>
 * <p>
 * <b>Implementation considerations:</b><br/>
 * 1. Implementation class should have default/non-arg contructor<br/>
 * 2. All methods except {@code configure(...)} and {@code close()} can be called from different threads. So, your code should be thread-safe.<br/>
 * 3. All methods will be executed in separate child-first classloader.<br/>
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
   * Reads configuration using property resolvers and sets up serde's internal state.
   *
   * @param serdeProperties        specific serde instance's properties
   * @param kafkaClusterProperties properties of the custer for what serde is instantiated
   * @param globalProperties       global application properties
   */
  void configure(
      PropertyResolver serdeProperties,
      PropertyResolver kafkaClusterProperties,
      PropertyResolver globalProperties
  );

  /**
   * Get serde's description.
   * @return Serde's description. Treated as Markdown text. Will be shown in UI.
   */
  Optional<String> getDescription();

  /**
   * Get schema description for specified topic's key/value.
   * @param topic topic name
   * @param type  {@code Target} for which {@code SchemaDescription} will be returned.
   * @return SchemaDescription for specified topic's key/value.
   * {@code Optional.empty} if there is not information about schema.
   */
  Optional<SchemaDescription> getSchema(String topic, Target type);

  /**
   * Checks if this Serde can be applied to specified topic's key/value deserialization.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Deserializer} will be applied.
   * @return true if this Serde can be applied to specified topic's key/value deserialization
   */
  boolean canDeserialize(String topic, Target type);

  /**
   * Checks if this Serde can be applied to specified topic's key/value serialization.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Serializer} will be applied.
   * @return true if this Serde can be applied to specified topic's key/value serialization
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
   * Creates {@code Serializer} for specified topic's key/value.
   * kafbat-ui doesn't cache  {@code Serializes} - new one will be created each time user's message needs to be serialized.
   * (Unless kafbat-ui supports batch inserts).
   * @param topic topic name
   * @param type  {@code Target} for which {@code Serializer} will be created.
   * @return {@code Serializer} for specified topic's key/value.
   */
  Serializer serializer(String topic, Target type);

  /**
   * Creates {@code Deserializer} for specified topic's key/value.
   * {@code Deserializer} will be created for each kafka polling and will be used for all messages within that polling cycle.
   * @param topic topic name
   * @param type  {@code Target} for which {@code Deserializer} will be created.
   * @return {@code Deserializer} for specified topic's key/value.
   */
  Deserializer deserializer(String topic, Target type);

  /**
   * Serializes client's input to {@code bytes[]} that will be sent to kafka as key/value (depending on what {@code Type} it was created for).
   */
  interface Serializer {

    /**
     * Serializes input string to bytes.
     * @param input string entered by user into UI text field.<br/> Note: this input is not formatted in any way.
     * @return serialized bytes. Can be null if input is null or empty string.
     */
    byte[] serialize(String input);

    /**
     * Serializes input string to bytes. Uses provided headers for additional information.
     * @param input string entered by user into UI text field.<br/> Note: this input is not formatted in any way.
     * @return serialized bytes. Can be null if input is null or empty string.
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
     * Deserializes record's key/value to string.
     * @param headers record's headers
     * @param data    record's key/value
     * @return deserialized object. Can be null if input is null or empty string.
     */
    DeserializeResult deserialize(RecordHeaders headers, byte[] data);
  }

}
