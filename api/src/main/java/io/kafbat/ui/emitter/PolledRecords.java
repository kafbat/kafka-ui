package io.kafbat.ui.emitter;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.utils.Bytes;

/** Holds Kafka poll results together with their size and elapsed-time measurements. */
public record PolledRecords(int count,
                            int bytes,
                            Duration elapsed,
                            ConsumerRecords<Bytes, Bytes> records) implements Iterable<ConsumerRecord<Bytes, Bytes>> {

  /** Wraps Kafka poll results with their record count, byte size, and elapsed time. */
  static PolledRecords create(ConsumerRecords<Bytes, Bytes> polled, Duration pollDuration) {
    return new PolledRecords(
        polled.count(),
        calculatePolledRecSize(polled),
        pollDuration,
        polled
    );
  }

  /** Returns records belonging to one topic partition. */
  public List<ConsumerRecord<Bytes, Bytes>> records(TopicPartition tp) {
    return records.records(tp);
  }

  /** Iterates over every record returned by the poll. */
  @Override
  public Iterator<ConsumerRecord<Bytes, Bytes>> iterator() {
    return records.iterator();
  }

  /** Returns the partitions represented in this poll. */
  public Set<TopicPartition> partitions() {
    return records.partitions();
  }

  /** Calculates the combined serialized size of a batch. */
  private static int calculatePolledRecSize(Iterable<ConsumerRecord<Bytes, Bytes>> recs) {
    int polledBytes = 0;
    for (ConsumerRecord<Bytes, Bytes> rec : recs) {
      polledBytes += calculateRecordSize(rec);
    }
    return polledBytes;
  }

  /** Calculates the serialized key, value, and header size of one record. */
  static int calculateRecordSize(ConsumerRecord<Bytes, Bytes> rec) {
    int bytes = 0;
    for (Header header : rec.headers()) {
      bytes +=
          (header.key() != null ? header.key().getBytes().length : 0)
              + (header.value() != null ? header.value().length : 0);
    }
    bytes += rec.key() == null ? 0 : rec.serializedKeySize();
    bytes += rec.value() == null ? 0 : rec.serializedValueSize();
    return bytes;
  }
}
