package io.kafbat.ui.service.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import io.kafbat.ui.model.TopicAnalysisStatsHourlyMsgCountsInnerDTO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.Test;

class TopicAnalysisStatsTest {

  private static final String TOPIC = "analysis-stats-test";

  /**
   * Guards https://github.com/kafbat/kafka-ui/issues/374 - a single-partition topic has to report exactly what
   * its only partition reports, the sparse 99.9th percentile included.
   */
  @Test
  void mergeOfSinglePartitionProducesStatsIdenticalToThatPartition() {
    var partitionStats = new TopicAnalysisStats();
    generateRecords(0, 100_000, new Random(7)).forEach(partitionStats::apply);

    var totalDto = TopicAnalysisStats.merge(List.of(partitionStats)).toDto(null);
    var partitionDto = partitionStats.toDto(0);

    assertThat(totalDto.getValueSize().getPrctl999()).isEqualTo(partitionDto.getValueSize().getPrctl999());
    assertThat(totalDto)
        .usingRecursiveComparison()
        .ignoringFields("partition")
        .isEqualTo(partitionDto);
  }

  @Test
  void mergeAggregatesStatsOfAllPartitions() {
    var partition0Records = generateRecords(0, 1_000, new Random(1));
    var partition1Records = generateRecords(1, 500, new Random(2));

    var partition0Stats = new TopicAnalysisStats();
    var partition1Stats = new TopicAnalysisStats();
    partition0Records.forEach(partition0Stats::apply);
    partition1Records.forEach(partition1Stats::apply);

    var totalDto = TopicAnalysisStats.merge(List.of(partition0Stats, partition1Stats)).toDto(null);
    var dto0 = partition0Stats.toDto(0);
    var dto1 = partition1Stats.toDto(1);

    assertThat(totalDto.getPartition()).isNull();
    assertThat(totalDto.getTotalMsgs()).isEqualTo(1_500);
    assertThat(totalDto.getMinOffset()).isEqualTo(Math.min(dto0.getMinOffset(), dto1.getMinOffset()));
    assertThat(totalDto.getMaxOffset()).isEqualTo(Math.max(dto0.getMaxOffset(), dto1.getMaxOffset()));
    assertThat(totalDto.getMinTimestamp()).isEqualTo(Math.min(dto0.getMinTimestamp(), dto1.getMinTimestamp()));
    assertThat(totalDto.getMaxTimestamp()).isEqualTo(Math.max(dto0.getMaxTimestamp(), dto1.getMaxTimestamp()));
    assertThat(totalDto.getNullKeys()).isEqualTo(dto0.getNullKeys() + dto1.getNullKeys());
    assertThat(totalDto.getNullValues()).isEqualTo(dto0.getNullValues() + dto1.getNullValues());

    assertThat(totalDto.getValueSize().getSum()).isEqualTo(dto0.getValueSize().getSum() + dto1.getValueSize().getSum());
    assertThat(totalDto.getValueSize().getMin())
        .isEqualTo(Math.min(dto0.getValueSize().getMin(), dto1.getValueSize().getMin()));
    assertThat(totalDto.getValueSize().getMax())
        .isEqualTo(Math.max(dto0.getValueSize().getMax(), dto1.getValueSize().getMax()));

    assertThat(totalDto.getHourlyMsgCounts().stream()
        .mapToLong(TopicAnalysisStatsHourlyMsgCountsInnerDTO::getCount).sum()).isEqualTo(1_500);

    // both partitions reuse the same key/value alphabet, so the merged sketches must deduplicate across them
    var allRecords = Stream.concat(partition0Records.stream(), partition1Records.stream()).toList();
    assertThat(totalDto.getApproxUniqKeys())
        .isCloseTo(distinctCount(allRecords, ConsumerRecord::key), withPercentage(10));
    assertThat(totalDto.getApproxUniqValues())
        .isCloseTo(distinctCount(allRecords, ConsumerRecord::value), withPercentage(10));
  }

  /**
   * The analysis creates a stats holder for every partition upfront, so untouched partitions join the merge empty.
   */
  @Test
  void mergeIgnoresPartitionsWithoutRecords() {
    var writtenPartition = new TopicAnalysisStats();
    generateRecords(1, 10_000, new Random(3)).forEach(writtenPartition::apply);

    var totalDto = TopicAnalysisStats
        .merge(List.of(new TopicAnalysisStats(), writtenPartition, new TopicAnalysisStats()))
        .toDto(null);

    assertThat(totalDto)
        .usingRecursiveComparison()
        .ignoringFields("partition")
        .isEqualTo(writtenPartition.toDto(1));
  }

  @Test
  void mergeOfTopicWithoutAnyRecordsReportsEmptyStats() {
    var totalDto = TopicAnalysisStats.merge(List.of(new TopicAnalysisStats(), new TopicAnalysisStats())).toDto(null);

    assertThat(totalDto.getTotalMsgs()).isZero();
    assertThat(totalDto.getMinOffset()).isNull();
    assertThat(totalDto.getMaxOffset()).isNull();
    assertThat(totalDto.getMinTimestamp()).isNull();
    assertThat(totalDto.getMaxTimestamp()).isNull();
    assertThat(totalDto.getHourlyMsgCounts()).isEmpty();
    // an untouched topic has to look exactly like it did before the stats were merged from partitions
    assertThat(totalDto)
        .usingRecursiveComparison()
        .ignoringFields("partition")
        .isEqualTo(new TopicAnalysisStats().toDto(null));
  }

  private static long distinctCount(List<ConsumerRecord<Bytes, Bytes>> records,
                                    Function<ConsumerRecord<Bytes, Bytes>, Bytes> extractor) {
    return records.stream()
        .map(extractor)
        .filter(Objects::nonNull)
        .distinct()
        .count();
  }

  private static List<ConsumerRecord<Bytes, Bytes>> generateRecords(int partition, int count, Random rnd) {
    long baseTs = Instant.now().minus(Duration.ofDays(3)).toEpochMilli();
    List<ConsumerRecord<Bytes, Bytes>> records = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      // most messages are of a similar size, with a sparse tail of much bigger ones - the shape that makes
      // the 99.9th percentile diverge between two independently built sketches
      int valueSize = rnd.nextDouble() < 0.995 ? 512 + rnd.nextInt(3) : 4096 + rnd.nextInt(60_000);
      Bytes key = i % 10 == 0 ? null : Bytes.wrap(("key-" + i).getBytes(StandardCharsets.UTF_8));
      Bytes value = i % 20 == 0 ? null : Bytes.wrap(("value-" + i).getBytes(StandardCharsets.UTF_8));
      records.add(new ConsumerRecord<>(
          TOPIC,
          partition,
          i,
          baseTs + i * 1_000L,
          TimestampType.CREATE_TIME,
          key == null ? -1 : key.get().length,
          value == null ? -1 : valueSize,
          key,
          value,
          new RecordHeaders(),
          Optional.empty()
      ));
    }
    return records;
  }

}
