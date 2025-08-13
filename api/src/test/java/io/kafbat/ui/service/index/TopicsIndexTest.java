package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class TopicsIndexTest {
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
                "reporting.payments.by.currencyid"
            )
            .map(s -> InternalTopic.builder().name(s).partitions(Map.of()).build()).toList());

    topics.addAll(
        List.of(
            InternalTopic.builder().name("configurable").partitions(Map.of()).topicConfigs(
                List.of(InternalTopicConfig.builder().name("retention").value("compact").build())
            ).build()
        )
    );

    int testTopicsCount = (int) topics.stream().filter(s -> s.getName().contains("topic")).count();

    Map<String, Integer> examples = Map.ofEntries(
        Map.entry("topic", testTopicsCount),
        Map.entry("8", 1),
        Map.entry("9", 0),
        Map.entry("tpic", testTopicsCount),
        Map.entry("dogs red", 1),
        Map.entry("tpic-1", 1),
        Map.entry("payments dlq", 1),
        Map.entry("paymnts dlq", 1),
        Map.entry("stats dlq", 0),
        Map.entry("stat", 3),
        Map.entry("chnges", 1),
        Map.entry("comands", 1),
        Map.entry("id", 1),
        Map.entry("config_retention:compact", 1)
    );

    SoftAssertions softly = new SoftAssertions();
    try(TopicsIndex index = new TopicsIndex(topics)) {
      for (Map.Entry<String, Integer> entry : examples.entrySet()) {
        List<String> resultAll = index.find(entry.getKey(), null, topics.size());
        softly.assertThat(resultAll.size())
            .withFailMessage("Expected %d results for '%s', but got %s", entry.getValue(), entry.getKey(), resultAll)
            .isEqualTo(entry.getValue());
      }
    }
    softly.assertAll();
  }

}
