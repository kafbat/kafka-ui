package io.kafbat.ui.serde.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides access to configuration properties.
 * Actual implementation uses {@code org.springframework.boot.context.properties.bind.Binder} class
 * to bind values to target types. Target type params can be custom config classes, not only simple types and strings.
 *
 */
public interface PropertyResolver {

  /**
   * Get property value by name.
   * @param <T> the type of the property
   * @param key property name
   * @param targetType type of property value
   * @return property value or empty {@code Optional} if property is not found
   */
  <T> Optional<T> getProperty(String key, Class<T> targetType);


  /**
   * Get list property value by name.
   *
   * @param <T> the type of the item
   * @param key list property name
   * @param itemType type of list element
   * @return list property value or empty {@code Optional} if property is not found
   */
  <T> Optional<List<T>> getListProperty(String key, Class<T> itemType);

  /**
   * Get map property value by name.
   *
   * @param key  map property name
   * @param keyType type of map key
   * @param valueType type of map value
   * @param <K> the type of the key
   * @param <V> the type of the value
   * @return map property value or empty {@code Optional} if property is not found
   */
  <K, V> Optional<Map<K, V>> getMapProperty(String key, Class<K> keyType, Class<V> valueType);

}
