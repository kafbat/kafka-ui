package io.kafbat.ui.serdes;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ClusterSerdesTest {

  private final StringSerde stringSerde = new StringSerde();

  private SerdeInstance autoConfiguredSerde(String name) {
    return new SerdeInstance(name, stringSerde, null, null, null, false);
  }

  private SerdeInstance autoConfiguredSerde(String name, Serde serde) {
    return new SerdeInstance(name, serde, null, null, null, false);
  }

  private SerdeInstance explicitSerdeNoPattern(String name, Serde serde) {
    return new SerdeInstance(name, serde, null, null, null, true);
  }

  private SerdeInstance explicitSerdeWithPattern(String name, Serde serde, String pattern) {
    var p = Pattern.compile(pattern);
    return new SerdeInstance(name, serde, p, p, null, true);
  }

  @Test
  void explicitlyConfiguredSerdeWithNullPatternIsSelectedWhenPreferable() {
    var preferableSerde = new AlwaysPreferableSerde();
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("CustomSR", explicitSerdeNoPattern("CustomSR", preferableSerde));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.KEY).getName())
        .isEqualTo("CustomSR");
    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.VALUE).getName())
        .isEqualTo("CustomSR");
  }

  @Test
  void autoConfiguredSerdeWithNullPatternIsNotAutoSelected() {
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("AutoSR", autoConfiguredSerde("AutoSR"));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.KEY).getName())
        .isEqualTo(StringSerde.NAME);
  }

  @Test
  void explicitSerdeWithPatternStillRequiresPatternMatch() {
    var preferableSerde = new AlwaysPreferableSerde();
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("CustomSR", explicitSerdeWithPattern("CustomSR", preferableSerde, "matching-.*"));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("matching-topic", Serde.Target.KEY).getName())
        .isEqualTo("CustomSR");
    assertThat(clusterSerdes.suggestSerdeForDeserialize("other-topic", Serde.Target.KEY).getName())
        .isEqualTo(StringSerde.NAME);
  }

  @Test
  void autoConfiguredPreferableSerdeIsAutoSelectedOverString() {
    // Regression pin for #1833: an auto-configured serde (no pattern, not explicitly configured)
    // that opts in via couldBePreferable must still be auto-selected over String. This mirrors an
    // auto-configured SchemaRegistry on a topic that has schemas.
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));
    serdes.put("SchemaRegistry", autoConfiguredSerde("SchemaRegistry", new AlwaysPreferableSerde()));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.KEY).getName())
        .isEqualTo("SchemaRegistry");
    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.VALUE).getName())
        .isEqualTo("SchemaRegistry");
  }

  @Test
  void explicitClusterDefaultWinsOverPreferableSerde() {
    // Pass 2 (cluster default) takes precedence over Pass 3 (couldBePreferable auto-detection).
    var defaultValue = autoConfiguredSerde(StringSerde.NAME);
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put(StringSerde.NAME, defaultValue);
    serdes.put("SchemaRegistry", autoConfiguredSerde("SchemaRegistry", new AlwaysPreferableSerde()));

    var clusterSerdes = new ClusterSerdes(serdes, null, defaultValue, autoConfiguredSerde("Fallback"));

    // VALUE has an explicit cluster default -> String wins.
    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.VALUE).getName())
        .isEqualTo(StringSerde.NAME);
    // KEY has no cluster default -> preferable serde wins via Pass 3.
    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.KEY).getName())
        .isEqualTo("SchemaRegistry");
  }

  @Test
  void preferableSerdeIsSkippedWhenItCannotDeserialize() {
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("SchemaRegistry",
        autoConfiguredSerde("SchemaRegistry", new PreferableButNotDeserializableSerde()));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.VALUE).getName())
        .isEqualTo(StringSerde.NAME);
  }

  @Test
  void firstRegisteredPreferableSerdeWins() {
    // Pass 3 iterates in registration order; the first opted-in serde wins.
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("FirstPreferable", autoConfiguredSerde("FirstPreferable", new AlwaysPreferableSerde()));
    serdes.put("SecondPreferable", autoConfiguredSerde("SecondPreferable", new AlwaysPreferableSerde()));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));

    var clusterSerdes = new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"));

    assertThat(clusterSerdes.suggestSerdeForDeserialize("any-topic", Serde.Target.VALUE).getName())
        .isEqualTo("FirstPreferable");
  }

  static class AlwaysPreferableSerde extends StringSerde {
    @Override
    public boolean couldBePreferable(String topic, Target type) {
      return true;
    }
  }

  static class PreferableButNotDeserializableSerde extends StringSerde {
    @Override
    public boolean couldBePreferable(String topic, Target type) {
      return true;
    }

    @Override
    public boolean canDeserialize(String topic, Target type) {
      return false;
    }
  }
}
