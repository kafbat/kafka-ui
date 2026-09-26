package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The index is long-lived and updated in place, so these tests focus on what an update must and must
 * not do: add new topics, drop removed ones, pick up changed ones, and leave untouched topics alone.
 */
class LuceneTopicsIndexUpdateTest {

  private static InternalTopic topic(String name) {
    return InternalTopic.builder().name(name).partitions(Map.of()).build();
  }

  private static InternalTopic topicWithPartitions(String name, int partitions) {
    return InternalTopic.builder().name(name).partitionCount(partitions)
        .partitions(IntStream.range(0, partitions)
            .mapToObj(i -> InternalPartition.builder().partition(i).build())
            .collect(Collectors.toMap(InternalPartition::getPartition, Function.identity())))
        .build();
  }

  private static InternalTopic topicWithConfigs(String name, String... nameValuePairs) {
    var configs = new ArrayList<InternalTopicConfig>();
    for (int i = 0; i < nameValuePairs.length; i += 2) {
      configs.add(InternalTopicConfig.builder()
          .name(nameValuePairs[i]).value(nameValuePairs[i + 1]).build());
    }
    return InternalTopic.builder().name(name).partitions(Map.of()).topicConfigs(configs).build();
  }

  private static List<String> names(List<InternalTopic> topics) {
    return topics.stream().map(InternalTopic::getName).sorted().toList();
  }

  @Test
  void findsTopicsIndexedByTheConstructor() throws Exception {
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(topic("sk.payment.events"), topic("audit.log")))) {
      assertThat(names(index.find("payment", null, true, 10))).containsExactly("sk.payment.events");
      assertThat(index.searchableDocCount()).isEqualTo(2);
    }
  }

  @Test
  void updateMakesNewTopicsSearchable() throws Exception {
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(topic("audit.log")))) {
      index.update(List.of(topic("audit.log"), topic("sk.payment.events")));

      assertThat(names(index.find("payment", null, true, 10))).containsExactly("sk.payment.events");
      assertThat(index.searchableDocCount()).isEqualTo(2);
    }
  }

  @Test
  void updateRemovesDeletedTopics() throws Exception {
    try (LuceneTopicsIndex index =
             new LuceneTopicsIndex(List.of(topic("audit.log"), topic("sk.payment.events")))) {
      index.update(List.of(topic("audit.log")));

      assertThat(index.find("payment", null, true, 10)).isEmpty();
      assertThat(index.find("audit", null, true, 10)).hasSize(1);
      assertThat(index.searchableDocCount()).isEqualTo(1);
    }
  }

  @Test
  void repeatedUpdateWithTheSameTopicsIsANoOp() throws Exception {
    var topics = List.of(topic("sk.payment.events"), topic("audit.log"), topicWithPartitions("wide.topic", 12));
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics)) {
      for (int i = 0; i < 5; i++) {
        index.update(topics);
      }

      assertThat(names(index.find("sk", null, true, 10))).containsExactly("sk.payment.events");
      assertThat(index.searchableDocCount()).isEqualTo(3);
    }
  }

  @Test
  void configOrderAloneDoesNotMakeTopicsDirty() throws Exception {
    var a = InternalTopic.builder().name("configurable").partitions(Map.of()).topicConfigs(List.of(
        InternalTopicConfig.builder().name("retention").value("compact").build(),
        InternalTopicConfig.builder().name("cleanup.policy").value("compact").build()
    )).build();
    var b = InternalTopic.builder().name("configurable").partitions(Map.of()).topicConfigs(List.of(
        InternalTopicConfig.builder().name("cleanup.policy").value("compact").build(),
        InternalTopicConfig.builder().name("retention").value("compact").build()
    )).build();

    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(a))) {
      index.update(List.of(b));

      assertThat(index.searchableDocCount()).isEqualTo(1);
      assertThat(index.find("config_retention:compact", null, true, 10)).hasSize(1);
    }
  }

  @Test
  void changedPartitionCountIsPickedUpByRangeQueries() throws Exception {
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(topic("wide.topic")))) {
      assertThat(index.find("partitions:{1 TO *}", null, true, 10)).isEmpty();

      index.update(List.of(topicWithPartitions("wide.topic", 10)));

      assertThat(index.find("partitions:{1 TO *}", null, true, 10)).hasSize(1);
      assertThat(index.find("partitions:{* TO 9}", null, true, 10)).isEmpty();
    }
  }

  @Test
  void changedConfigValueIsPickedUp() throws Exception {
    try (LuceneTopicsIndex index =
             new LuceneTopicsIndex(List.of(topicWithConfigs("configurable", "retention", "delete")))) {
      assertThat(index.find("config_retention:delete", null, true, 10)).hasSize(1);
      assertThat(index.find("config_retention:compact", null, true, 10)).isEmpty();

      index.update(List.of(topicWithConfigs("configurable", "retention", "compact")));

      assertThat(index.find("config_retention:delete", null, true, 10)).isEmpty();
      assertThat(index.find("config_retention:compact", null, true, 10)).hasSize(1);
    }
  }

  @Test
  void topicReplacedUnderTheSameNameIsNotDuplicated() throws Exception {
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(topic("rolling.topic")))) {
      index.update(List.of(topic("rolling.topic")));
      index.update(List.of(topic("rolling.topic")));

      assertThat(index.find("rolling", null, true, 10)).hasSize(1);
      assertThat(index.searchableDocCount()).isEqualTo(1);
    }
  }

  @Test
  void equallyScoredTopicsAreOrderedByNameAndStayStableAcrossUpdates() throws Exception {
    var topics = List.of(topic("sk.payment.events.dlq"), topic("sk.payment.events"));
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics)) {
      assertThat(names(index.find("payment events", null, true, 10)))
          .containsExactly("sk.payment.events", "sk.payment.events.dlq");

      // re-indexing the same topics in a different order must not reshuffle equal scores
      index.update(List.of(topic("sk.payment.events"), topic("sk.payment.events.dlq"), topic("unrelated")));
      index.update(List.of(topic("unrelated"), topic("sk.payment.events.dlq"), topic("sk.payment.events")));

      assertThat(names(index.find("payment events", null, true, 10)))
          .containsExactly("sk.payment.events", "sk.payment.events.dlq");
    }
  }

  @ParameterizedTest
  @EnumSource(SearchMode.class)
  void updatedIndexKeepsAnsweringPlainSearches(SearchMode mode) throws Exception {
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(List.of(topic("sk.payment.events"), topic("audit.log")))) {
      index.update(List.of(topic("sk.payment.events"), topic("audit.log"), topic("new.topic")));

      assertThat(names(index.find("topic", null, mode.fts, 10))).containsExactly("new.topic");
      assertThat(names(index.find("payment", null, mode.fts, 10))).containsExactly("sk.payment.events");
    }
  }

  enum SearchMode {
    PLAIN(false), FTS(true);

    final boolean fts;

    SearchMode(boolean fts) {
      this.fts = fts;
    }
  }
}
