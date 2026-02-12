package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.Test;

class PolledRecordsTest {

  @Test
  void bytesOfEmptyIsZero() {
    assertThat(PolledRecords.bytesOf(List.of())).isZero();
  }

  @Test
  void bytesOfCountsSerializedSizes() {
    // bytesOf uses key/value presence: when non-null, adds serializedKeySize/serializedValueSize
    ConsumerRecord<Bytes, Bytes> rec = new ConsumerRecord<>(
        "t",
        0,
        0L,
        0L,
        TimestampType.CREATE_TIME,
        3,
        5,
        Bytes.wrap(new byte[3]),
        Bytes.wrap(new byte[5]),
        new RecordHeaders(),
        Optional.empty()
    );
    assertThat(PolledRecords.bytesOf(List.of(rec))).isEqualTo(3 + 5);
  }

  @Test
  void bytesOfSumsMultipleRecords() {
    ConsumerRecord<Bytes, Bytes> r1 = new ConsumerRecord<>(
        "t", 0, 0L, 0L, TimestampType.CREATE_TIME, 2, 4,
        Bytes.wrap(new byte[1]), Bytes.wrap(new byte[1]), new RecordHeaders(), Optional.empty());
    ConsumerRecord<Bytes, Bytes> r2 = new ConsumerRecord<>(
        "t", 0, 1L, 0L, TimestampType.CREATE_TIME, 1, 1,
        Bytes.wrap(new byte[1]), Bytes.wrap(new byte[1]), new RecordHeaders(), Optional.empty());
    assertThat(PolledRecords.bytesOf(List.of(r1, r2))).isEqualTo(2 + 4 + 1 + 1);
  }
}
