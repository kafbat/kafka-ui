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
              calculateLag(
                  Optional.ofNullable(e.getValue()),
                  Optional.ofNullable(endOffsets.get(e.getKey()))
              ).orElse(0L)
          ).sum();
    }

    return consumerLag;
  }

  public static Optional<Long> calculateLag(Optional<Long> commitedOffset, Optional<Long> endOffset) {
    Optional<Long> consumerLag = Optional.empty();
    if (endOffset.isPresent()) {
      consumerLag = commitedOffset.map(o -> endOffset.get() - o);
    }
    return consumerLag;
  }
}
