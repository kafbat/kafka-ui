package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kafbat.ui.model.ConsumerPosition;
import io.kafbat.ui.model.PollingModeDTO;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class CursorTrackingTest {

  @Test
  void forwardCursorAdvancesOnlyPartitionsThatReadARecord() {
    var topic = "topic";
    var capturedCursor = new AtomicReference<Cursor>();
    var tracking = new Cursor.Tracking(
        mock(ConsumerRecordDeserializer.class),
        new ConsumerPosition(PollingModeDTO.EARLIEST, topic, List.of(), null, null),
        _ -> true,
        100,
        cursor -> {
          capturedCursor.set(cursor);
          return "cursor";
        });

    tracking.initOffsets(Map.of(
        new TopicPartition(topic, 0), 10L,
        new TopicPartition(topic, 1), 20L));
    tracking.trackOffset(topic, 0, 12L);
    tracking.registerCursor();

    assertThat(capturedCursor.get().consumerPosition().offsets().tpOffsets())
        .containsEntry(new TopicPartition(topic, 0), 13L)
        .containsEntry(new TopicPartition(topic, 1), 20L);
  }
}
