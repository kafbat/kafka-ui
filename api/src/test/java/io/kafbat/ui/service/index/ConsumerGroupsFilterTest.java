package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.junit.jupiter.api.Test;

class ConsumerGroupsFilterTest {
  private final List<String> names = List.of(
      "connect-test-10",
      "connect-test-9",
      "connect-test-90",
      "connect-local-file-sink",
      "martech-mt0-data-integration-platform-rapi-orders",
      "pmp-recon-connect.payments.crypto.ctt.agent.created.v1",
      "rmd-flink.volatility.trigger.v26",
      "iat-ntapi-account-dd-consumer-prod"
  );

  @Test
  void testFindTopicsByName() throws Exception {
    List<ConsumerGroupListing> groups =
        names.stream().map(n -> new ConsumerGroupListing(n, true)).toList();

    ConsumerGroupFilter filter = new ConsumerGroupFilter(groups);

    Map<String, Integer> tests = Map.of(
        "test 10", 1,
        "test", 3,
        "payment created", 1,
        "test 9", 2,
        "volatility 26", 1
    );

    for (Map.Entry<String, Integer> entry : tests.entrySet()) {
      List<ConsumerGroupListing> result = filter.find(entry.getKey());
      assertThat(result).size()
          .withFailMessage("Expected %d results for '%s', but got %s", entry.getValue(), entry.getKey(), result)
          .isEqualTo(entry.getValue());
    }
  }

}
