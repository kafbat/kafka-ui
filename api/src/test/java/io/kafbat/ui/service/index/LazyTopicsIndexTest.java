package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LazyTopicsIndexTest {

  private static final List<InternalTopic> TOPICS = List.of(
      InternalTopic.builder().name("sk.payment.events").partitions(Map.of()).build(),
      InternalTopic.builder().name("sk.payment.events.dlq").partitions(Map.of()).build(),
      InternalTopic.builder().name("sk.payment.commands").partitions(Map.of()).build(),
      InternalTopic.builder().name("sk.payment.stats").partitions(Map.of()).build(),
      InternalTopic.builder().name("sk.currency.rates").partitions(Map.of()).build(),
      InternalTopic.builder().name("audit.clients.state").partitions(Map.of()).build(),
      InternalTopic.builder().name("configurable")
          .partitions(Map.of())
          .topicConfigs(List.of(InternalTopicConfig.builder().name("retention").value("compact").build()))
          .build(),
      InternalTopic.builder().name("multiple_parts").partitionCount(10)
          .partitions(IntStream.range(0, 10)
              .mapToObj(i -> InternalPartition.builder().partition(i).build())
              .collect(Collectors.toMap(InternalPartition::getPartition, p -> p)))
          .build()
  );

  @Test
  void plainSearchBehavesLikeFilterIndex() throws Exception {
    try (var lazy = new LazyTopicsIndex(TOPICS);
         var filter = new FilterTopicIndex(TOPICS)) {
      for (String search : Arrays.asList(null, "", "payment", "PAYMENT", "stat", "sk.payment", "dlq", "nomatch")) {
        assertThat(lazy.find(search, true, TopicsIndex.FIELD_NAME, false, null))
            .as("plain search for %s", search)
            .isEqualTo(filter.find(search, true, TopicsIndex.FIELD_NAME, false, null));
      }
    }
  }

  @Test
  void ftsSearchBehavesLikeLuceneIndex() throws Exception {
    try (var lazy = new LazyTopicsIndex(TOPICS);
         var lucene = new LuceneTopicsIndex(TOPICS)) {
      for (String search : Arrays.asList(null, "payment", "stat", "sk.payment.events", "dlq", "nomatch")) {
        assertThat(lazy.find(search, true, TopicsIndex.FIELD_NAME, true, null))
            .as("fts search for %s", search)
            .isEqualTo(lucene.find(search, true, TopicsIndex.FIELD_NAME, true, null));
      }
    }
  }

  @Test
  void ftsSearchIsOrderedByRelevance() throws Exception {
    try (var lazy = new LazyTopicsIndex(TOPICS)) {
      var found = lazy.find("payment events", true, TopicsIndex.FIELD_NAME, true, null).stream()
          .map(InternalTopic::getName)
          .toList();
      assertThat(found).containsExactly("sk.payment.events", "sk.payment.events.dlq");
    }
  }

  @Test
  void emptyResultIsEmptyForBothModes() throws Exception {
    try (var lazy = new LazyTopicsIndex(TOPICS)) {
      assertThat(lazy.find("definitely-not-there", true, TopicsIndex.FIELD_NAME, false, null)).isEmpty();
      assertThat(lazy.find("definitely-not-there", true, TopicsIndex.FIELD_NAME, true, null)).isEmpty();
    }
  }

  @Test
  void closeIsIdempotentWhenLuceneWasNeverBuilt() throws Exception {
    var lazy = new LazyTopicsIndex(TOPICS);
    lazy.find("payment", true, TopicsIndex.FIELD_NAME, false, null);
    assertThatCode(lazy::close).doesNotThrowAnyException();
  }
}
