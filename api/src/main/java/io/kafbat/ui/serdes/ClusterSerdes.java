package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import jakarta.annotation.Nullable;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ClusterSerdes implements Closeable {

  final Map<String, SerdeInstance> serdes;

  @Nullable
  final SerdeInstance defaultKeySerde;

  @Nullable
  final SerdeInstance defaultValueSerde;

  @Getter
  final SerdeInstance fallbackSerde;

  // evaluated in order, the first rule matching the topic applies
  final List<ProduceRestriction> produceRestrictions;

  ClusterSerdes(Map<String, SerdeInstance> serdes,
                @Nullable SerdeInstance defaultKeySerde,
                @Nullable SerdeInstance defaultValueSerde,
                SerdeInstance fallbackSerde) {
    this(serdes, defaultKeySerde, defaultValueSerde, fallbackSerde, List.of());
  }

  private Optional<SerdeInstance> findSerdeByPatternsOrDefault(String topic,
                                                               Serde.Target type,
                                                               Predicate<SerdeInstance> additionalCheck) {
    // iterating over serdes in the same order they were added in config
    for (SerdeInstance serdeInstance : serdes.values()) {
      if (!serdeInstance.couldBePreferable(topic, type)) {
        continue;
      }
      var pattern = type == Serde.Target.KEY
          ? serdeInstance.topicKeyPattern
          : serdeInstance.topicValuePattern;
      boolean topicMatches = pattern != null
          ? pattern.matcher(topic).matches()
          : serdeInstance.explicitlyConfigured;
      if (topicMatches && additionalCheck.test(serdeInstance)) {
        return Optional.of(serdeInstance);
      }
    }
    if (type == Serde.Target.KEY
        && defaultKeySerde != null
        && additionalCheck.test(defaultKeySerde)) {
      return Optional.of(defaultKeySerde);
    }
    if (type == Serde.Target.VALUE
        && defaultValueSerde != null
        && additionalCheck.test(defaultValueSerde)) {
      return Optional.of(defaultValueSerde);
    }
    return Optional.empty();
  }

  public Optional<SerdeInstance> serdeForName(String name) {
    return Optional.ofNullable(serdes.get(name));
  }

  public Stream<SerdeInstance> all() {
    return serdes.values().stream();
  }

  public SerdeInstance suggestSerdeForSerialize(String topic, Serde.Target type) {
    return findSerdeByPatternsOrDefault(topic, type, s -> s.canSerialize(topic, type))
        .orElse(serdes.get(StringSerde.NAME));
  }

  /**
   * Checks whether the serde may be used to produce the topic's key or value,
   * according to the first produce restriction matching the topic (if any).
   */
  public boolean isAllowedForProduce(String topic, Serde.Target type, String serdeName) {
    return produceRestrictions.stream()
        .filter(r -> r.appliesTo(topic))
        .findFirst()
        .map(r -> r.allows(type, serdeName))
        .orElse(true);
  }

  /**
   * Serdes that can be used to produce the topic's key or value, preferred serde first.
   * The preferred serde is the suggested one when it is allowed for producing,
   * otherwise the first allowed serde. Empty if no serde is allowed.
   */
  public List<SerdeInstance> serdesForSerialize(String topic, Serde.Target type) {
    var allowed = serdes.values().stream()
        .filter(s -> isAllowedForProduce(topic, type, s.getName()))
        .filter(s -> s.canSerialize(topic, type))
        .toList();
    var suggested = suggestSerdeForSerialize(topic, type);
    var result = new ArrayList<SerdeInstance>(allowed.size() + 1);
    if (suggested != null && isAllowedForProduce(topic, type, suggested.getName())) {
      result.add(suggested);
    }
    allowed.stream()
        .filter(s -> suggested == null || !s.getName().equals(suggested.getName()))
        .forEach(result::add);
    return result;
  }

  public SerdeInstance suggestSerdeForDeserialize(String topic, Serde.Target type) {
    return findSerdeByPatternsOrDefault(topic, type, s -> s.canDeserialize(topic, type))
        .orElse(serdes.get(StringSerde.NAME));
  }

  @Override
  public void close() {
    serdes.values().forEach(SerdeInstance::close);
  }
}
