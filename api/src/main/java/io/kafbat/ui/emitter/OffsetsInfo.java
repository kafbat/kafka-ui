package io.kafbat.ui.emitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.UnsupportedVersionException;

@Slf4j
@Getter
class OffsetsInfo {

  private final Consumer<?, ?> consumer;

  private final Map<TopicPartition, Long> beginOffsets;
  private final Map<TopicPartition, Long> endOffsets;

  private final Set<TopicPartition> nonEmptyPartitions = new HashSet<>();
  private final Set<TopicPartition> emptyPartitions = new HashSet<>();

  OffsetsInfo(Consumer<?, ?> consumer, String topic) {
    this(consumer,
        consumer.partitionsFor(topic).stream()
            .map(pi -> new TopicPartition(topic, pi.partition()))
            .toList()
    );
  }

  OffsetsInfo(Consumer<?, ?> consumer, Collection<TopicPartition> targetPartitions) {
    this.consumer = consumer;
    this.beginOffsets = firstOffsetsForPolling(consumer, targetPartitions);
    this.endOffsets = consumer.endOffsets(targetPartitions);
    endOffsets.forEach((tp, endOffset) -> {
      var beginningOffset = beginOffsets.get(tp);
      if (endOffset > beginningOffset) {
        nonEmptyPartitions.add(tp);
      } else {
        emptyPartitions.add(tp);
      }
    });
  }


  private Map<TopicPartition, Long> firstOffsetsForPolling(Consumer<?, ?> consumer,
                                                           Collection<TopicPartition> partitions) {
    try {
      // we try to use offsetsForTimes() to find earliest offsets, since for
      // some topics (like compacted) beginningOffsets() ruturning 0 offsets
      // even when effectively first offset can be very high
      var offsets = consumer.offsetsForTimes(
          partitions.stream().collect(Collectors.toMap(p -> p, p -> 0L))
      );
      // result of offsetsForTimes() can be null, if message version < 0.10.0
      if (offsets.entrySet().stream().noneMatch(e -> e.getValue() == null)) {
        return offsets.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
      }
    } catch (UnsupportedOperationException | UnsupportedVersionException e) {
      // offsetsForTimes() not supported
    }
    //falling back to beginningOffsets() if offsetsForTimes() not supported
    return consumer.beginningOffsets(partitions);
  }

  boolean assignedPartitionsFullyPolled() {
    for (var tp : consumer.assignment()) {
      Preconditions.checkArgument(endOffsets.containsKey(tp));
      if (endOffsets.get(tp) > consumer.position(tp)) {
        return false;
      }
    }
    return true;
  }

  long summaryOffsetsRange() {
    MutableLong cnt = new MutableLong();
    nonEmptyPartitions.forEach(tp -> cnt.add(endOffsets.get(tp) - beginOffsets.get(tp)));
    return cnt.getValue();
  }

  public Set<TopicPartition> allTargetPartitions() {
    return Sets.union(nonEmptyPartitions, emptyPartitions);
  }

}
