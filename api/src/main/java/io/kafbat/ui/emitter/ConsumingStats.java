package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageBlockedDTO;
import io.kafbat.ui.model.TopicMessageConsumingDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.model.TopicMessageNextPageCursorDTO;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import reactor.core.publisher.FluxSink;

class ConsumingStats {

  private final long bytesLimit;
  private long bytes = 0;
  private int records = 0;
  private long elapsed = 0;
  private int filterApplyErrors = 0;
  private boolean bytesLimitReached = false;
  private TopicMessageBlockedDTO blockedMessage;

  enum ConsumptionResult {
    CONSUMED,
    BYTE_LIMIT_REACHED,
    RECORD_TOO_LARGE
  }

  /** Creates a statistics accumulator with a byte limit; non-positive limits are unlimited. */
  ConsumingStats(long bytesLimit) {
    this.bytesLimit = bytesLimit;
  }

  /** Accumulates poll time and emits current consumption statistics. */
  void sendConsumingEvt(FluxSink<TopicMessageEventDTO> sink, PolledRecords polledRecords) {
    elapsed += polledRecords.elapsed().toMillis();
    sink.next(
        new TopicMessageEventDTO()
            .type(TopicMessageEventDTO.TypeEnum.CONSUMING)
            .consuming(createConsumingStats())
    );
  }

  /** Records a message-filter evaluation failure. */
  void incFilterApplyError() {
    filterApplyErrors++;
  }

  /** Reports whether any record reached the configured byte limit. */
  boolean bytesLimitReached() {
    return bytesLimitReached;
  }

  /** Accounts for a record and distinguishes a batch limit from a record that cannot fit. */
  ConsumptionResult tryConsumeRecord(ConsumerRecord<Bytes, Bytes> kafkaRecord) {
    int recordBytes = PolledRecords.calculateRecordSize(kafkaRecord);
    if (bytesLimit > 0 && recordBytes > bytesLimit) {
      bytes = Math.max(bytes, bytesLimit);
      bytesLimitReached = true;
      blockedMessage = new TopicMessageBlockedDTO()
          .partition(kafkaRecord.partition())
          .offset(kafkaRecord.offset())
          .timestamp(kafkaRecord.timestamp() < 0
              ? null
              : OffsetDateTime.ofInstant(
                  Instant.ofEpochMilli(kafkaRecord.timestamp()), ZoneOffset.UTC))
          .size((long) recordBytes);
      return ConsumptionResult.RECORD_TOO_LARGE;
    }
    if (bytesLimit > 0 && bytes + recordBytes > bytesLimit) {
      bytes = Math.max(bytes, bytesLimit);
      bytesLimitReached = true;
      return ConsumptionResult.BYTE_LIMIT_REACHED;
    }

    bytes += recordBytes;
    records++;
    if (bytesLimit > 0 && bytes >= bytesLimit) {
      bytesLimitReached = true;
    }
    return ConsumptionResult.CONSUMED;
  }

  /** Emits final statistics and registers a continuation cursor when polling can resume. */
  void sendFinishEvent(FluxSink<TopicMessageEventDTO> sink, @Nullable Cursor.Tracking cursor) {
    sink.next(
        new TopicMessageEventDTO()
            .type(TopicMessageEventDTO.TypeEnum.DONE)
            .cursor(
                cursor != null
                    ? new TopicMessageNextPageCursorDTO().id(cursor.registerCursor())
                    : null
            )
            .consuming(createConsumingStats())
    );
  }

  /** Builds the public statistics payload from the accumulated counters. */
  private TopicMessageConsumingDTO createConsumingStats() {
    return new TopicMessageConsumingDTO()
        .bytesConsumed(bytes)
        .bytesLimit(bytesLimit)
        .bytesLimitReached(bytesLimitReached())
        .blockedMessage(blockedMessage)
        .elapsedMs(elapsed)
        .isCancelled(false)
        .filterApplyErrors(filterApplyErrors)
        .messagesConsumed(records);
  }
}
