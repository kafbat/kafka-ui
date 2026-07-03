package io.kafbat.ui.emitter;

import io.kafbat.ui.model.TopicMessageEventDTO;
import jakarta.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import reactor.core.publisher.FluxSink;

abstract class AbstractEmitter implements java.util.function.Consumer<FluxSink<TopicMessageEventDTO>> {

  private final MessagesProcessing messagesProcessing;
  private final PollingSettings pollingSettings;

  protected AbstractEmitter(MessagesProcessing messagesProcessing, PollingSettings pollingSettings) {
    this.messagesProcessing = messagesProcessing;
    this.pollingSettings = pollingSettings;
  }

  protected PolledRecords poll(FluxSink<TopicMessageEventDTO> sink, EnhancedConsumer consumer) {
    return consumer.pollEnhanced(pollingSettings.getPollTimeout());
  }

  protected boolean isSendLimitReached() {
    return messagesProcessing.limitReached();
  }

  protected boolean isBytesLimitReached() {
    return messagesProcessing.bytesLimitReached();
  }

  protected void send(FluxSink<TopicMessageEventDTO> sink,
                       Iterable<ConsumerRecord<Bytes, Bytes>> records,
                       @Nullable Cursor.Tracking cursor) {
    messagesProcessing.send(sink, records, cursor, false);
  }

  protected void sendAndTrackConsumption(FluxSink<TopicMessageEventDTO> sink,
                                         Iterable<ConsumerRecord<Bytes, Bytes>> records,
                                         @Nullable Cursor.Tracking cursor) {
    messagesProcessing.send(sink, records, cursor, true);
  }

  protected boolean tryConsumeRecord(ConsumerRecord<Bytes, Bytes> record) {
    return messagesProcessing.tryConsumeRecord(record);
  }

  protected void sendPhase(FluxSink<TopicMessageEventDTO> sink, String name) {
    messagesProcessing.sendPhase(sink, name);
  }

  protected void sendConsuming(FluxSink<TopicMessageEventDTO> sink, PolledRecords records) {
    messagesProcessing.sentConsumingInfo(sink, records);
  }

  // cursor is null if target partitions were fully polled (no, need to do paging)
  protected void sendFinishStatsAndCompleteSink(FluxSink<TopicMessageEventDTO> sink, @Nullable Cursor.Tracking cursor) {
    messagesProcessing.sendFinishEvents(sink, cursor);
    sink.complete();
  }
}
