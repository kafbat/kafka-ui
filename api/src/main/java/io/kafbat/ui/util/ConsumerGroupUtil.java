package io.kafbat.ui.util;

import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.TopicPartition;

public class ConsumerGroupUtil {
  private ConsumerGroupUtil() {
  }

  public static Long calculateConsumerLag(Map<TopicPartition, Long> offsets,
                                          Map<TopicPartition, Long> endOffsets) {
    Long consumerLag = null;
    // consumerLag should be undefined if no committed offsets found for topic
    if (!offsets.isEmpty()) {
      consumerLag = offsets.entrySet().stream()
          .mapToLong(e ->
              Optional.ofNullable(endOffsets)
                  .map(o -> o.get(e.getKey()))
                  .map(o -> o - e.getValue())
                  .orElse(0L)
          ).sum();
    }

    return consumerLag;
  }
}
