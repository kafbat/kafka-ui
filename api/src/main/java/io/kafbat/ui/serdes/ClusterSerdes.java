package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

  private Optional<SerdeInstance> findSerdeByPatternsOrDefault(String topic,
                                                               Serde.Target type,
                                                               Predicate<SerdeInstance> additionalCheck) {
    // Pass 1: explicit topic-pattern match, or an explicitly-configured serde without a pattern.
    // Iterating over serdes in the same order they were added in config.
    for (SerdeInstance serdeInstance : serdes.values()) {
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

    // Pass 2: cluster-level explicit default serde.
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

    // Pass 3: implicit auto-detection. A serde opts in via couldBePreferable when it's a good
    // default for this topic (e.g. SchemaRegistry when the registry has an applicable subject).
    // Only serdes without an explicit topic pattern are considered here - a configured pattern
    // scopes a serde to matching topics, so it must not be auto-selected elsewhere. The actual
    // per-message magic-byte check in the deserializer, plus the fallback serde, corrects any
    // topic whose messages turn out not to match.
    for (SerdeInstance serdeInstance : serdes.values()) {
      var pattern = type == Serde.Target.KEY
          ? serdeInstance.topicKeyPattern
          : serdeInstance.topicValuePattern;
      if (pattern == null
          && serdeInstance.couldBePreferable(topic, type)
          && additionalCheck.test(serdeInstance)) {
        return Optional.of(serdeInstance);
      }
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

  public SerdeInstance suggestSerdeForDeserialize(String topic, Serde.Target type) {
    return findSerdeByPatternsOrDefault(topic, type, s -> s.canDeserialize(topic, type))
        .orElse(serdes.get(StringSerde.NAME));
  }

  @Override
  public void close() {
    serdes.values().forEach(SerdeInstance::close);
  }
}
