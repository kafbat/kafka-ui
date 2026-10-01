package io.kafbat.ui.emitter;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.model.TopicMessagePhaseDTO;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import reactor.core.publisher.FluxSink;

@Slf4j
class MessagesProcessing {

  private long sentMessages = 0;

  private final ConsumerRecordDeserializer deserializer;
  private final Predicate<TopicMessageDTO> filter;
  private final boolean ascendingSortBeforeSend;
  private final @Nullable Integer limit;
  private final ConsumingStats consumingStats;

  /** Creates the ordered deserialization, filtering, and accounting pipeline for a poll. */
  MessagesProcessing(ConsumerRecordDeserializer deserializer,
                     Predicate<TopicMessageDTO> filter,
                     boolean ascendingSortBeforeSend,
                     @Nullable Integer limit,
                     long bytesLimit) {
    this.deserializer = deserializer;
    this.filter = filter;
    this.ascendingSortBeforeSend = ascendingSortBeforeSend;
    this.limit = limit;
    this.consumingStats = new ConsumingStats(bytesLimit);
  }

  /** Returns whether the page has emitted its allowed number of messages. */
  boolean limitReached() {
    return limit != null && sentMessages >= limit;
  }

  /** Returns whether further records would exceed the byte budget. */
  boolean bytesLimitReached() {
    return consumingStats.bytesLimitReached();
  }

  /** Orders, admits, deserializes, filters, and emits a batch of records. */
  void send(FluxSink<TopicMessageEventDTO> sink,
             Iterable<ConsumerRecord<Bytes, Bytes>> polled,
             @Nullable Cursor.Tracking cursor,
             boolean trackConsumption) {
    for (ConsumerRecord<Bytes, Bytes> kafkaRecord : sortForSending(polled, ascendingSortBeforeSend)) {
      if (!sendRecord(sink, kafkaRecord, cursor, trackConsumption)) {
        break;
      }
    }
  }

  private boolean sendRecord(FluxSink<TopicMessageEventDTO> sink,
                             ConsumerRecord<Bytes, Bytes> kafkaRecord,
                             @Nullable Cursor.Tracking cursor,
                             boolean trackConsumption) {
    if (limitReached() || sink.isCancelled()) {
      return false;
    }
    if (trackConsumption && !admitRecord(kafkaRecord, cursor)) {
      return false;
    }

    TopicMessageDTO topicMessage = deserializer.deserialize(kafkaRecord);
    try {
      if (filter.test(topicMessage)) {
        sink.next(
            new TopicMessageEventDTO()
                .type(TopicMessageEventDTO.TypeEnum.MESSAGE)
                .message(topicMessage)
        );
        sentMessages++;
      }
      if (cursor != null) {
        cursor.trackOffset(kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset());
      }
    } catch (Exception e) {
      consumingStats.incFilterApplyError();
      log.trace("Error applying filter for message {}", topicMessage);
    }
    return true;
  }

  private boolean admitRecord(ConsumerRecord<Bytes, Bytes> kafkaRecord,
                              @Nullable Cursor.Tracking cursor) {
    var result = consumingStats.tryConsumeRecord(kafkaRecord);
    if (result == ConsumingStats.ConsumptionResult.CONSUMED) {
      return true;
    }
    if (result == ConsumingStats.ConsumptionResult.RECORD_TOO_LARGE && cursor != null) {
      cursor.trackOffset(kafkaRecord.topic(), kafkaRecord.partition(), kafkaRecord.offset());
    }
    return false;
  }

  /** Emits statistics for one completed Kafka poll. */
  void sentConsumingInfo(FluxSink<TopicMessageEventDTO> sink, PolledRecords polledRecords) {
    if (!sink.isCancelled()) {
      consumingStats.sendConsumingEvt(sink, polledRecords);
    }
  }

  /** Emits terminal statistics and an optional continuation cursor. */
  void sendFinishEvents(FluxSink<TopicMessageEventDTO> sink, @Nullable Cursor.Tracking cursor) {
    if (!sink.isCancelled()) {
      consumingStats.sendFinishEvent(sink, cursor);
    }
  }

  /** Emits a named progress event. */
  void sendPhase(FluxSink<TopicMessageEventDTO> sink, String name) {
    if (!sink.isCancelled()) {
      sink.next(
          new TopicMessageEventDTO()
              .type(TopicMessageEventDTO.TypeEnum.PHASE)
              .phase(new TopicMessagePhaseDTO().name(name))
      );
    }
  }

  /**
   * Sorts by timestamp while preserving offset order within each partition.
   */
  @VisibleForTesting
  static Iterable<ConsumerRecord<Bytes, Bytes>> sortForSending(Iterable<ConsumerRecord<Bytes, Bytes>> records,
                                                               boolean asc) {
    Comparator<ConsumerRecord<Bytes, Bytes>> offsetComparator = asc
        ? Comparator.comparingLong(ConsumerRecord::offset)
        : Comparator.<ConsumerRecord<Bytes, Bytes>>comparingLong(ConsumerRecord::offset).reversed();

    // partition -> sorted by offsets records
    Map<Integer, List<ConsumerRecord<Bytes, Bytes>>> perPartition = Streams.stream(records)
        .collect(
            groupingBy(
                ConsumerRecord::partition,
                TreeMap::new,
                collectingAndThen(toList(), lst -> lst.stream().sorted(offsetComparator).toList())));

    Comparator<ConsumerRecord<Bytes, Bytes>> tsComparator = asc
        ? Comparator.comparing(ConsumerRecord::timestamp)
        : Comparator.<ConsumerRecord<Bytes, Bytes>>comparingLong(ConsumerRecord::timestamp).reversed();

    // merge-sorting records from partitions one by one using timestamp comparator
    return Iterables.mergeSorted(perPartition.values(), tsComparator);
  }

}
