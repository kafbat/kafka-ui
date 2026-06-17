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

  static class AlwaysPreferableSerde extends StringSerde {
    @Override
    public boolean couldBePreferable(String topic, Target type) {
      return true;
    }
  }
}
