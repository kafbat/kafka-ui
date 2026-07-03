package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageConsumingDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.model.TopicMessageNextPageCursorDTO;
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

  ConsumingStats(long bytesLimit) {
    this.bytesLimit = bytesLimit;
  }

  void sendConsumingEvt(FluxSink<TopicMessageEventDTO> sink, PolledRecords polledRecords) {
    elapsed += polledRecords.elapsed().toMillis();
    sink.next(
        new TopicMessageEventDTO()
            .type(TopicMessageEventDTO.TypeEnum.CONSUMING)
            .consuming(createConsumingStats())
    );
  }

  void incFilterApplyError() {
    filterApplyErrors++;
  }

  boolean bytesLimitReached() {
    return bytesLimitReached;
  }

  boolean tryConsumeRecord(ConsumerRecord<Bytes, Bytes> record) {
    int recordBytes = PolledRecords.calculateRecordSize(record);
    if (bytesLimit <= 0 || bytes + recordBytes > bytesLimit) {
      bytes = Math.max(bytes, bytesLimit);
      bytesLimitReached = true;
      return false;
    }

    bytes += recordBytes;
    records++;
    if (bytes >= bytesLimit) {
      bytesLimitReached = true;
    }
    return true;
  }

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

  private TopicMessageConsumingDTO createConsumingStats() {
    return new TopicMessageConsumingDTO()
        .bytesConsumed(bytes)
        .bytesLimit(bytesLimit)
        .bytesLimitReached(bytesLimitReached())
        .elapsedMs(elapsed)
        .isCancelled(false)
        .filterApplyErrors(filterApplyErrors)
        .messagesConsumed(records);
  }
}
