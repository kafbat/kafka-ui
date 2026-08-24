package io.kafbat.ui.service.index.lucene;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import io.kafbat.ui.service.index.LuceneTopicsIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LuceneTopicsIndexTest {
  @Test
  void testFindTopicsByName() throws Exception {
    List<InternalTopic> topics = new ArrayList<>(
        Stream.of("topic", "topic-1", "topic-2", "topic-3",
                "topic-4", "topic-5", "topic-6", "topic-7",
                "topic-8", "red-dog",
                "sk.payment.events",
                "sk.payment.events.dlq",
                "sk.payment.commands",
                "sk.payment.changes",
                "sk.payment.stats",
                "sk.currency.rates",
                "audit.payment.events",
                "audit.clients.state",
                "audit.clients.repartitioned.status",
                "reporting.payments.by.clientId",
                "reporting.payments.by.currencyId"
            )
            .map(s -> InternalTopic.builder().name(s).partitions(Map.of()).build()).toList());

    topics.addAll(
        List.of(
            InternalTopic.builder().name("configurable").partitions(Map.of()).topicConfigs(
                List.of(InternalTopicConfig.builder().name("retention").value("compact").build())
            ).build(),
            InternalTopic.builder().name("multiple_parts").partitionCount(10).partitions(
                IntStream.range(0, 10).mapToObj(i ->
                    InternalPartition.builder().partition(i).build()
                ).collect(Collectors.toMap(
                    InternalPartition::getPartition,
                    Function.identity()
                ))
            ).build()
        )
    );

    int testTopicsCount = (int) topics.stream().filter(s -> s.getName().contains("topic")).count();

    Map<String, Integer> examples = Map.ofEntries(
        Map.entry("topic", testTopicsCount),
        Map.entry("8", 1),
        Map.entry("9", 0),
        Map.entry("dog red", 1),
        Map.entry("topic-1", 1),
        Map.entry("payment dlq", 1),
        Map.entry("stats dlq", 0),
        Map.entry("stat", 3),
        Map.entry("changes", 1),
        Map.entry("commands", 1),
        Map.entry("id", 2),
        Map.entry("config_retention:compact", 1),
        Map.entry("partitions:10", 1),
        Map.entry("partitions:{1 TO *]", 1),
        Map.entry("partitions:{* TO 9]", topics.size() - 1)
    );

    SoftAssertions softly = new SoftAssertions();
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics)) {
      for (Map.Entry<String, Integer> entry : examples.entrySet()) {
        List<InternalTopic> resultAll = index.find(entry.getKey(), null, true, topics.size());
        softly.assertThat(resultAll.size())
            .withFailMessage("Expected %d results for '%s', but got %s", entry.getValue(), entry.getKey(), resultAll)
            .isEqualTo(entry.getValue());
      }
    }
    softly.assertAll();
  }

  @ParameterizedTest
  @MethodSource("providerOrdered")
  void testOrders(List<String> orderedTopics, String search) throws Exception {
    List<InternalTopic> topics = orderedTopics.stream()
            .map(s -> InternalTopic.builder().name(s).partitions(Map.of()).build()).toList();


    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics)) {
      List<InternalTopic> resultAll = index.find(search, null, true, topics.size());
      assertThat(resultAll).isEqualTo(topics);
    }
  }

  public static Stream<Arguments> providerOrdered() {
    return Stream.of(
        Arguments.of(List.of("sk.long.term.name", "long.sk", "longnamebefore.sk"), "sk"),
        Arguments.of(List.of("sk_long_term.name", "sk", "sk2", "long-sk", "longnamebeforeSk"), "sk")
    );
  }

}
