package io.kafbat.ui.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InternalClusterStateTest {

  @Test
  void usesPerBrokerRatesWhenPresent() {
    var result = InternalClusterState.sumWithTopicFallback(
        Map.of(1, new BigDecimal("10"), 2, new BigDecimal("5")),
        Map.of("ignored", new BigDecimal("999")));
    assertThat(result).isEqualByComparingTo("15");
  }

  @Test
  void fallsBackToTopicRatesWhenPerBrokerRatesEmpty() {
    // brokers that don't expose the topic-less BrokerTopicMetrics aggregate (e.g. cp-kafka over JMX)
    var result = InternalClusterState.sumWithTopicFallback(
        Map.of(),
        Map.of("topicA", new BigDecimal("3.0"), "topicB", new BigDecimal("4.5")));
    assertThat(result).isEqualByComparingTo("7.5");
  }

  @Test
  void returnsNullWhenNeitherSourceHasRates() {
    assertThat(InternalClusterState.sumWithTopicFallback(Map.of(), Map.of())).isNull();
  }
}
