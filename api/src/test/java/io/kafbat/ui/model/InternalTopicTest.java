package io.kafbat.ui.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class InternalTopicTest {

  @ParameterizedTest
  @EnumSource(value = CleanupPolicy.class, names = {"DELETE", "UNKNOWN"})
  void messagesCountIsSumOfPartitionOffsetRanges(CleanupPolicy policy) {
    assertThat(topic(policy).getMessagesCount()).isEqualTo(15L);
  }

  @ParameterizedTest
  @EnumSource(value = CleanupPolicy.class, names = {"COMPACT", "COMPACT_DELETE"})
  void messagesCountIsNullForCompactedTopics(CleanupPolicy policy) {
    assertThat(topic(policy).getMessagesCount()).isNull();
  }

  private static InternalTopic topic(CleanupPolicy policy) {
    return InternalTopic.builder()
        .name("topic")
        .cleanUpPolicy(policy)
        .partitions(Map.of(
            0, InternalPartition.builder().partition(0).offsetMin(0L).offsetMax(10L).build(),
            1, InternalPartition.builder().partition(1).offsetMin(100L).offsetMax(105L).build()
        ))
        .build();
  }
}
