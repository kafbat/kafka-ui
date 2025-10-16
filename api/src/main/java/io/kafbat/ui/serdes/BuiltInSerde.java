package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import java.util.Optional;

public interface BuiltInSerde extends Serde {

  // returns true is serde has enough properties set on cluster&global levels to
  // be configured without explicit config provide
  default boolean canBeAutoConfigured(PropertyResolver kafkaClusterProperties,
                                      PropertyResolver globalProperties) {
    return true;
  }

  // will be called for build-in serdes that were not explicitly registered
  // and that returned true on canBeAutoConfigured(..) call.
  // NOTE: Serde.configure() method won't be called if serde is auto-configured!
  default void autoConfigure(PropertyResolver kafkaClusterProperties,
                             PropertyResolver globalProperties) {
  }

  @Override
  default void configure(PropertyResolver serdeProperties,
                         PropertyResolver kafkaClusterProperties,
                         PropertyResolver globalProperties) {
  }

  @Override
  default boolean canSerialize(String topic, Serde.Target type) {
    return false;
  }

  @Override
  default Serde.Serializer serializer(String topic, Serde.Target type) {
    throw new UnsupportedOperationException();
  }

  @Override
  default Optional<SchemaDescription> getSchema(String topic, Serde.Target type) {
    return Optional.empty();
  }

  @Override
  default Optional<String> getDescription() {
    return Optional.empty();
  }

}
