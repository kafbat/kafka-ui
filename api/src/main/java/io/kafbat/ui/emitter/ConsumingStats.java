package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageConsumingDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.model.TopicMessagePageCursorDTO;
import reactor.core.publisher.FluxSink;

class ConsumingStats {

  private long bytes = 0;
  private int records = 0;
  private long elapsed = 0;
  private int filterApplyErrors = 0;

  void sendConsumingEvt(FluxSink<TopicMessageEventDTO> sink, PolledRecords polledRecords) {
    bytes += polledRecords.bytes();
    records += polledRecords.count();
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

  void sendFinishEvent(FluxSink<TopicMessageEventDTO> sink, Cursor.Tracking cursor, boolean hasNext) {
    String previousCursorId = cursor.getPreviousCursorId();
    sink.next(
        new TopicMessageEventDTO()
            .type(TopicMessageEventDTO.TypeEnum.DONE)
            .prevCursor(
                previousCursorId != null
                    ? new TopicMessagePageCursorDTO().id(previousCursorId)
                    : null
            )
            .nextCursor(
                hasNext
                    ? new TopicMessagePageCursorDTO().id(cursor.registerCursor())
                    : null
            )
            .consuming(createConsumingStats())
    );
  }

  private TopicMessageConsumingDTO createConsumingStats() {
    return new TopicMessageConsumingDTO()
        .bytesConsumed(bytes)
        .elapsedMs(elapsed)
        .isCancelled(false)
        .filterApplyErrors(filterApplyErrors)
        .messagesConsumed(records);
  }
}
