package io.kafbat.ui.emitter;

import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.TopicMessageEventDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.utils.Bytes;
import reactor.core.publisher.FluxSink;

@Slf4j
abstract class RangePollingEmitter extends AbstractEmitter {

  private final Supplier<EnhancedConsumer> consumerSupplier;
  private final Cursor.Tracking cursor;
  protected final ConsumerPosition consumerPosition;
  protected final int messagesPerPage;

  /** Creates a range-based emitter with page state and cursor tracking. */
  protected RangePollingEmitter(Supplier<EnhancedConsumer> consumerSupplier,
                                ConsumerPosition consumerPosition,
                                int messagesPerPage,
                                MessagesProcessing messagesProcessing,
                                PollingSettings pollingSettings,
                                Cursor.Tracking cursor) {
    super(messagesProcessing, pollingSettings);
    this.consumerPosition = consumerPosition;
    this.messagesPerPage = messagesPerPage;
    this.consumerSupplier = consumerSupplier;
    this.cursor = cursor;
  }

  protected record FromToOffset(/*inclusive*/ long from, /*exclusive*/ long to) {
  }

  //should return empty map if polling should be stopped
  /** Selects the next bounded offset range for each active partition. */
  protected abstract TreeMap<TopicPartition, FromToOffset> nextPollingRange(
      TreeMap<TopicPartition, FromToOffset> prevRange, //empty on start
      SeekOperations seekOperations
  );

  /** Polls ranges until the page, byte budget, or requested range is exhausted. */
  @Override
  public void accept(FluxSink<TopicMessageEventDTO> sink) {
    log.debug("Starting polling for {}", consumerPosition);
    try (EnhancedConsumer consumer = consumerSupplier.get()) {
      sendPhase(sink, "Consumer created");
      var seekOperations = SeekOperations.create(consumer, consumerPosition);
      cursor.initOffsets(seekOperations.getOffsetsForSeek());

      TreeMap<TopicPartition, FromToOffset> pollRange = nextPollingRange(new TreeMap<>(), seekOperations);
      log.debug("Starting from offsets {}", pollRange);

      while (!sink.isCancelled() && !pollRange.isEmpty() && !isSendLimitReached() && !isBytesLimitReached()) {
        var polled = poll(consumer, sink, pollRange);
        sendAndTrackConsumption(sink, polled, cursor);
        if (!isBytesLimitReached()) {
          pollRange = nextPollingRange(pollRange, seekOperations);
        }
      }
      if (sink.isCancelled()) {
        log.debug("Polling finished due to sink cancellation");
      }
      sendFinishStatsAndCompleteSink(sink, pollRange.isEmpty() ? null : cursor);
      log.debug("Polling finished");
    } catch (InterruptException kafkaInterruptException) {
      log.debug("Polling finished due to thread interruption");
      sink.complete();
    } catch (Exception e) {
      log.error("Error occurred while consuming records", e);
      sink.error(e);
    }
  }

  /** Collects records from each partition without admitting them before global ordering. */
  private List<ConsumerRecord<Bytes, Bytes>> poll(EnhancedConsumer consumer,
                                                  FluxSink<TopicMessageEventDTO> sink,
                                                  TreeMap<TopicPartition, FromToOffset> range) {
    log.trace("Polling range {}", range);
    sendPhase(sink,
        "Polling partitions: %s".formatted(range.keySet().stream().map(TopicPartition::partition).sorted().toList()));

    consumer.assign(range.keySet());
    range.forEach((tp, fromTo) -> consumer.seek(tp, fromTo.from));

    List<ConsumerRecord<Bytes, Bytes>> result = new ArrayList<>();
    Set<TopicPartition> paused = new HashSet<>();
    while (!sink.isCancelled() && paused.size() < range.size()) {
      var polledRecords = poll(sink, consumer);
      for (var entry : range.entrySet()) {
        var tp = entry.getKey();
        var fromTo = entry.getValue();
        for (var kafkaRecord : polledRecords.records(tp)) {
          if (kafkaRecord.offset() >= fromTo.to) {
            continue;
          }
          result.add(kafkaRecord);
        }

        //next position is out of target range -> pausing partition
        if (!paused.contains(tp) && consumer.position(tp) >= fromTo.to) {
          paused.add(tp);
          consumer.pause(List.of(tp));
        }
      }
      sendConsuming(sink, polledRecords);
    }
    consumer.resume(paused);
    return result;
  }
}
