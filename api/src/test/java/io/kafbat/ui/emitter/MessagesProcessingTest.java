package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class MessagesProcessingTest {

  @Test
  void oversizedRecordAdvancesCursorWithoutDeserializing() {
    var deserializer = mock(ConsumerRecordDeserializer.class);
    var cursor = mock(Cursor.Tracking.class);
    var processing = new MessagesProcessing(deserializer, message -> true, true, 10, 4);
    var kafkaRecord = new ConsumerRecord<Bytes, Bytes>(
        "topic", 2, 42, 0, TimestampType.CREATE_TIME, 0, 5, null,
        Bytes.wrap(new byte[5]), new RecordHeaders(), Optional.empty());

    Flux.<TopicMessageEventDTO>create(sink -> {
      processing.send(sink, List.of(kafkaRecord), cursor, true);
      sink.complete();
    }).blockLast();

    verify(cursor).trackOffset("topic", 2, 42);
    verifyNoInteractions(deserializer);
  }

  @Test
  void byteLimitIsAppliedAfterRecordsAreSortedForDelivery() {
    var deserializer = mock(ConsumerRecordDeserializer.class);
    var cursor = mock(Cursor.Tracking.class);
    when(deserializer.deserialize(any())).thenReturn(new TopicMessageDTO());
    var processing = new MessagesProcessing(deserializer, message -> true, true, 10, 3);
    var laterRecord = consumerRecord(0, 1, "2000-01-02T00:00:00+00:00", 3);
    var earlierRecord = consumerRecord(1, 0, "2000-01-01T00:00:00+00:00", 3);

    Flux.<TopicMessageEventDTO>create(sink -> {
      processing.send(sink, List.of(laterRecord, earlierRecord), cursor, true);
      sink.complete();
    }).blockLast();

    verify(cursor).trackOffset("topic", 1, 0);
    verify(deserializer).deserialize(earlierRecord);
    verifyNoMoreInteractions(deserializer);
  }


  @RepeatedTest(5)
  void testSortingAsc() {
    var messagesInOrder = List.of(
        consumerRecord(1, 100L, "1999-01-01T00:00:00+00:00"),
        consumerRecord(0, 0L, "2000-01-01T00:00:00+00:00"),
        consumerRecord(1, 200L, "2000-01-05T00:00:00+00:00"),
        consumerRecord(0, 10L, "2000-01-10T00:00:00+00:00"),
        consumerRecord(0, 20L, "2000-01-20T00:00:00+00:00"),
        consumerRecord(1, 300L, "3000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1000L, "4000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1001L, "2000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1003L, "3000-01-01T00:00:00+00:00")
    );

    var shuffled = new ArrayList<>(messagesInOrder);
    Collections.shuffle(shuffled);

    var sortedList = MessagesProcessing.sortForSending(shuffled, true);
    assertThat(sortedList).containsExactlyElementsOf(messagesInOrder);
  }

  @RepeatedTest(5)
  void testSortingDesc() {
    var messagesInOrder = List.of(
        consumerRecord(1, 300L, "3000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1003L, "3000-01-01T00:00:00+00:00"),
        consumerRecord(0, 20L, "2000-01-20T00:00:00+00:00"),
        consumerRecord(0, 10L, "2000-01-10T00:00:00+00:00"),
        consumerRecord(1, 200L, "2000-01-05T00:00:00+00:00"),
        consumerRecord(0, 0L, "2000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1001L, "2000-01-01T00:00:00+00:00"),
        consumerRecord(2, 1000L, "4000-01-01T00:00:00+00:00"),
        consumerRecord(1, 100L, "1999-01-01T00:00:00+00:00")
    );

    var shuffled = new ArrayList<>(messagesInOrder);
    Collections.shuffle(shuffled);

    var sortedList = MessagesProcessing.sortForSending(shuffled, false);
    assertThat(sortedList).containsExactlyElementsOf(messagesInOrder);
  }

  private ConsumerRecord<Bytes, Bytes> consumerRecord(int partition, long offset, String ts) {
    return consumerRecord(partition, offset, ts, 0);
  }

  private ConsumerRecord<Bytes, Bytes> consumerRecord(
      int partition, long offset, String ts, int valueSize) {
    return new ConsumerRecord<>(
        "topic", partition, offset, OffsetDateTime.parse(ts).toInstant().toEpochMilli(),
        TimestampType.CREATE_TIME,
        0, valueSize, null,
        valueSize == 0 ? null : Bytes.wrap(new byte[valueSize]),
        new RecordHeaders(), Optional.empty()
    );
  }

}
