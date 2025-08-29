package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

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
        Map.entry("id", 2)
    );

    SoftAssertions softly = new SoftAssertions();
    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics)) {
      for (Map.Entry<String, Integer> entry : examples.entrySet()) {
        List<InternalTopic> resultAll = index.find(entry.getKey(), null, topics.size());
        softly.assertThat(resultAll.size())
            .withFailMessage("Expected %d results for '%s', but got %s", entry.getValue(), entry.getKey(), resultAll)
            .isEqualTo(entry.getValue());
      }
    }

    HashMap<String, Integer> indexExamples = new HashMap<>(examples);
    indexExamples.put("config_retention:compact", 1);
    indexExamples.put("partitions:10", 1);
    indexExamples.put("partitions:{1 TO *]", 1);
    indexExamples.put("partitions:{* TO 9]", topics.size() - 1);

    try (LuceneTopicsIndex index = new LuceneTopicsIndex(topics,
        new ClustersProperties.FtsProperties(false, 1, 4))) {
      for (Map.Entry<String, Integer> entry : indexExamples.entrySet()) {
        List<InternalTopic> resultAll = index.find(entry.getKey(), null, topics.size());
        softly.assertThat(resultAll.size())
            .withFailMessage("Expected %d results for '%s', but got %s", entry.getValue(), entry.getKey(), resultAll)
            .isEqualTo(entry.getValue());
      }
    }
    softly.assertAll();
  }

}
