package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.TopicMessageEventDTO;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class ConsumingStatsTest {

  @Test
  void reportsRecordThatExceedsEntireByteLimit() {
    var stats = new ConsumingStats(4);
    var kafkaRecord = createRecord(2, 42, 1, 4);

    assertThat(stats.tryConsumeRecord(kafkaRecord))
        .isEqualTo(ConsumingStats.ConsumptionResult.RECORD_TOO_LARGE);

    var events = Flux.<TopicMessageEventDTO>create(sink -> {
      stats.sendFinishEvent(sink, null);
      sink.complete();
    }).collectList().block();

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getConsuming().getBlockedMessage())
        .satisfies(blocked -> {
          assertThat(blocked.getPartition()).isEqualTo(2);
          assertThat(blocked.getOffset()).isEqualTo(42);
          assertThat(blocked.getSize()).isGreaterThan(4);
        });
  }

  @Test
  void doesNotReportRecordWhenOnlyRemainingBatchBudgetIsExceeded() {
    var stats = new ConsumingStats(10);
    var first = createRecord(0, 0, 0, 6);
    var second = createRecord(0, 1, 0, 6);

    assertThat(stats.tryConsumeRecord(first))
        .isEqualTo(ConsumingStats.ConsumptionResult.CONSUMED);
    assertThat(stats.tryConsumeRecord(second))
        .isEqualTo(ConsumingStats.ConsumptionResult.BYTE_LIMIT_REACHED);

    var events = Flux.<TopicMessageEventDTO>create(sink -> {
      stats.sendFinishEvent(sink, null);
      sink.complete();
    }).collectList().block();

    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getConsuming().getBlockedMessage()).isNull();
  }

  @Test
  void nonPositiveByteLimitAllowsRecords() {
    assertThat(new ConsumingStats(0).tryConsumeRecord(createRecord(0, 0, 0, 6)))
        .isEqualTo(ConsumingStats.ConsumptionResult.CONSUMED);
    assertThat(new ConsumingStats(-1).tryConsumeRecord(createRecord(0, 0, 0, 6)))
        .isEqualTo(ConsumingStats.ConsumptionResult.CONSUMED);
  }

  private ConsumerRecord<Bytes, Bytes> createRecord(
      int partition, long offset, int keySize, int valueSize) {
    return new ConsumerRecord<>(
        "topic",
        partition,
        offset,
        0,
        TimestampType.CREATE_TIME,
        keySize,
        valueSize,
        keySize == 0 ? null : Bytes.wrap(new byte[keySize]),
        Bytes.wrap(new byte[valueSize]),
        new RecordHeaders(),
        Optional.empty());
  }
}
