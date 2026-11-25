package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.Serde;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Serdes allowed to produce messages to topics matching {@code topicPattern}.
 * A null set leaves that side (key or value) unrestricted.
 */
record ProduceRestriction(Pattern topicPattern,
                          @Nullable Set<String> keySerdes,
                          @Nullable Set<String> valueSerdes) {

  boolean appliesTo(String topic) {
    return topicPattern.matcher(topic).matches();
  }

  boolean allows(Serde.Target type, String serdeName) {
    var allowed = type == Serde.Target.KEY ? keySerdes : valueSerdes;
    return allowed == null || allowed.contains(serdeName);
  }
}
