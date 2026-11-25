package io.kafbat.ui.serdes;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
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

  @Test
  void produceIsUnrestrictedWithoutMatchingRule() {
    var clusterSerdes = clusterSerdesWithRestrictions(
        restriction("domain\\..*", Set.of("SR"), Set.of("SR"))
    );

    assertThat(clusterSerdes.isAllowedForProduce("other-topic", Serde.Target.KEY, StringSerde.NAME)).isTrue();
    assertThat(clusterSerdes.isAllowedForProduce("other-topic", Serde.Target.VALUE, StringSerde.NAME)).isTrue();
    // pattern must match the whole topic name
    assertThat(clusterSerdes.isAllowedForProduce("x.domain.topic", Serde.Target.VALUE, StringSerde.NAME)).isTrue();
  }

  @Test
  void produceRestrictionAppliesToKeyAndValueIndependently() {
    var clusterSerdes = clusterSerdesWithRestrictions(
        restriction("domain\\..*", null, Set.of("SR"))
    );

    assertThat(clusterSerdes.isAllowedForProduce("domain.topic", Serde.Target.KEY, StringSerde.NAME)).isTrue();
    assertThat(clusterSerdes.isAllowedForProduce("domain.topic", Serde.Target.VALUE, StringSerde.NAME)).isFalse();
    assertThat(clusterSerdes.isAllowedForProduce("domain.topic", Serde.Target.VALUE, "SR")).isTrue();
  }

  @Test
  void firstMatchingProduceRestrictionWins() {
    var clusterSerdes = clusterSerdesWithRestrictions(
        restriction("domain\\.legacy\\..*", Set.of(StringSerde.NAME), Set.of(StringSerde.NAME)),
        restriction("domain\\..*", Set.of("SR"), Set.of("SR"))
    );

    assertThat(clusterSerdes.isAllowedForProduce("domain.legacy.topic", Serde.Target.VALUE, StringSerde.NAME))
        .isTrue();
    assertThat(clusterSerdes.isAllowedForProduce("domain.topic", Serde.Target.VALUE, StringSerde.NAME))
        .isFalse();
  }

  @Test
  void serdesForSerializeListsOnlyAllowedSerdesWithPreferredFirst() {
    var clusterSerdes = clusterSerdesWithRestrictions(
        restriction("domain\\..*", null, Set.of("SR", "Other"))
    );

    // String is the suggested serde but isn't allowed: the first allowed serde becomes preferred
    assertThat(clusterSerdes.serdesForSerialize("domain.topic", Serde.Target.VALUE))
        .extracting(SerdeInstance::getName)
        .containsExactly("SR", "Other");
    assertThat(clusterSerdes.serdesForSerialize("domain.topic", Serde.Target.KEY))
        .extracting(SerdeInstance::getName)
        .containsExactly(StringSerde.NAME, "SR", "Other");
    assertThat(clusterSerdes.serdesForSerialize("other-topic", Serde.Target.VALUE))
        .extracting(SerdeInstance::getName)
        .containsExactly(StringSerde.NAME, "SR", "Other");
  }

  private ClusterSerdes clusterSerdesWithRestrictions(ProduceRestriction... restrictions) {
    var serdes = new LinkedHashMap<String, SerdeInstance>();
    serdes.put("SR", autoConfiguredSerde("SR"));
    serdes.put(StringSerde.NAME, autoConfiguredSerde(StringSerde.NAME));
    serdes.put("Other", autoConfiguredSerde("Other"));
    return new ClusterSerdes(serdes, null, null, autoConfiguredSerde("Fallback"), List.of(restrictions));
  }

  private ProduceRestriction restriction(String topicPattern, Set<String> keySerdes, Set<String> valueSerdes) {
    return new ProduceRestriction(Pattern.compile(topicPattern), keySerdes, valueSerdes);
  }

  static class AlwaysPreferableSerde extends StringSerde {
    @Override
    public boolean couldBePreferable(String topic, Target type) {
      return true;
    }
  }
}
