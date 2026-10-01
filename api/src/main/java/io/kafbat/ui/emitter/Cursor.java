package io.kafbat.ui.emitter;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.kafka.common.TopicPartition;

/** Captures the deserialization and seek state required to continue a message page. */
public record Cursor(ConsumerRecordDeserializer deserializer,
                     ConsumerPosition consumerPosition,
                     Predicate<TopicMessageDTO> filter,
                     int limit) {

  public static class Tracking {
    private final ConsumerRecordDeserializer deserializer;
    private final ConsumerPosition originalPosition;
    private final Predicate<TopicMessageDTO> filter;
    private final int limit;
    private final Function<Cursor, String> registerAction;

    //topic -> partition -> offset
    private final Table<String, Integer, Long> trackingOffsets = HashBasedTable.create();
    private final Set<TopicPartition> trackedPartitions = new HashSet<>();

    /** Tracks offsets and continuation state while one page is being consumed. */
    public Tracking(ConsumerRecordDeserializer deserializer,
                    ConsumerPosition originalPosition,
                    Predicate<TopicMessageDTO> filter,
                    int limit,
                    Function<Cursor, String> registerAction) {
      this.deserializer = deserializer;
      this.originalPosition = originalPosition;
      this.filter = filter;
      this.limit = limit;
      this.registerAction = registerAction;
    }

    /** Records the last consumed offset for a partition. */
    void trackOffset(String topic, int partition, long offset) {
      trackingOffsets.put(topic, partition, offset);
      trackedPartitions.add(new TopicPartition(topic, partition));
    }

    /** Seeds a partition's resume position before any records are consumed. */
    private void initOffset(String topic, int partition, long offset) {
      trackingOffsets.put(topic, partition, offset);
    }

    /** Initializes all partition offsets from the consumer's seek plan. */
    void initOffsets(Map<TopicPartition, Long> initialSeekOffsets) {
      initialSeekOffsets.forEach((tp, off) -> initOffset(tp.topic(), tp.partition(), off));
    }

    /** Builds cursor offsets, advancing only partitions that produced a record. */
    private Map<TopicPartition, Long> getOffsetsMap(boolean advanceTrackedPartitions) {
      Map<TopicPartition, Long> result = new HashMap<>();
      trackingOffsets.rowMap()
          .forEach((topic, partsMap) ->
              partsMap.forEach((partition, offset) -> {
                var topicPartition = new TopicPartition(topic, partition);
                result.put(
                    topicPartition,
                    offset + (advanceTrackedPartitions && trackedPartitions.contains(topicPartition) ? 1 : 0)
                );
              }));
      return result;
    }

    /** Registers and returns a cursor that resumes this polling operation. */
    String registerCursor() {
      return registerAction.apply(
          new Cursor(
              deserializer,
              new ConsumerPosition(
                  switch (originalPosition.pollingMode()) {
                    case TO_OFFSET, TO_TIMESTAMP, LATEST -> PollingModeDTO.TO_OFFSET;
                    case FROM_OFFSET, FROM_TIMESTAMP, EARLIEST -> PollingModeDTO.FROM_OFFSET;
                    case TAILING -> throw new IllegalStateException();
                  },
                  originalPosition.topic(),
                  originalPosition.partitions(),
                  null,
                  new ConsumerPosition.Offsets(
                      null,
                      getOffsetsMap(switch (originalPosition.pollingMode()) {
                        case TO_OFFSET, TO_TIMESTAMP, LATEST -> false;
                        // Forward polling resumes after the last record read in each partition.
                        case FROM_OFFSET, FROM_TIMESTAMP, EARLIEST -> true;
                        case TAILING -> throw new IllegalStateException();
                      })
                  )
              ),
              filter,
              limit
          )
      );
    }
  }

}
