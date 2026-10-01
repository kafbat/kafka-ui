package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageEventDTO;
import jakarta.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import reactor.core.publisher.FluxSink;

abstract class AbstractEmitter implements java.util.function.Consumer<FluxSink<TopicMessageEventDTO>> {

  private final MessagesProcessing messagesProcessing;
  private final PollingSettings pollingSettings;

  /** Creates an emitter that shares message processing and polling configuration. */
  protected AbstractEmitter(MessagesProcessing messagesProcessing, PollingSettings pollingSettings) {
    this.messagesProcessing = messagesProcessing;
    this.pollingSettings = pollingSettings;
  }

  /** Polls records using the configured timeout. */
  protected PolledRecords poll(FluxSink<TopicMessageEventDTO> sink, EnhancedConsumer consumer) {
    return consumer.pollEnhanced(pollingSettings.getPollTimeout());
  }

  /** Returns whether the configured message-count limit has been reached. */
  protected boolean isSendLimitReached() {
    return messagesProcessing.limitReached();
  }

  /** Returns whether the configured byte limit has been reached. */
  protected boolean isBytesLimitReached() {
    return messagesProcessing.bytesLimitReached();
  }

  /** Sends records without charging them against the byte-consumption budget. */
  protected void send(FluxSink<TopicMessageEventDTO> sink,
                       Iterable<ConsumerRecord<Bytes, Bytes>> records,
                       @Nullable Cursor.Tracking cursor) {
    messagesProcessing.send(sink, records, cursor, false);
  }

  /** Sends records in delivery order while applying byte admission and cursor tracking. */
  protected void sendAndTrackConsumption(FluxSink<TopicMessageEventDTO> sink,
                                         Iterable<ConsumerRecord<Bytes, Bytes>> records,
                                         @Nullable Cursor.Tracking cursor) {
    messagesProcessing.send(sink, records, cursor, true);
  }

  /** Emits a progress phase unless the consumer has cancelled the stream. */
  protected void sendPhase(FluxSink<TopicMessageEventDTO> sink, String name) {
    messagesProcessing.sendPhase(sink, name);
  }

  /** Emits statistics for a completed Kafka poll. */
  protected void sendConsuming(FluxSink<TopicMessageEventDTO> sink, PolledRecords records) {
    messagesProcessing.sentConsumingInfo(sink, records);
  }

  /**
   * Emits final statistics and completes the event stream.
   * The cursor is null when every target partition was fully polled.
   */
  protected void sendFinishStatsAndCompleteSink(FluxSink<TopicMessageEventDTO> sink, @Nullable Cursor.Tracking cursor) {
    messagesProcessing.sendFinishEvents(sink, cursor);
    sink.complete();
  }
}
