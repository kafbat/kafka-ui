package io.kafbat.ui.service.analyze;

import io.kafbat.ui.model.TopicAnalysisSizeStatsDTO;
import io.kafbat.ui.model.TopicAnalysisStatsDTO;
import io.kafbat.ui.model.TopicAnalysisStatsHourlyMsgCountsInnerDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.Union;
import org.apache.datasketches.quantiles.DoublesSketch;
import org.apache.datasketches.quantiles.DoublesUnion;
import org.apache.datasketches.quantiles.UpdateDoublesSketch;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;

class TopicAnalysisStats {

  Long totalMsgs = 0L;
  Long minOffset;
  Long maxOffset;

  Long minTimestamp;
  Long maxTimestamp;

  long nullKeys = 0L;
  long nullValues = 0L;

  final SizeStats keysSize = new SizeStats();
  final SizeStats valuesSize = new SizeStats();

  HllSketch uniqKeys = new HllSketch();
  HllSketch uniqValues = new HllSketch();

  final HourlyCounts hourlyCounts = new HourlyCounts();

  static class SizeStats {
    long sum = 0;
    Long min;
    Long max;
    UpdateDoublesSketch sizeSketch = DoublesSketch.builder().build();

    void apply(int len) {
      sum += len;
      min = minNullable(min, len);
      max = maxNullable(max, len);
      sizeSketch.update(len);
    }

    void merge(SizeStats other) {
      sum += other.sum;
      min = minNullable(min, other.min);
      max = maxNullable(max, other.max);
      var union = DoublesUnion.builder().build();
      union.update(sizeSketch);
      union.update(other.sizeSketch);
      sizeSketch = union.getResult();
    }

    TopicAnalysisSizeStatsDTO toDto() {
      return new TopicAnalysisSizeStatsDTO()
          .sum(sum)
          .min(min)
          .max(max)
          .avg((long) (((double) sum) / sizeSketch.getN()))
          .prctl50((long) sizeSketch.getQuantile(0.5))
          .prctl75((long) sizeSketch.getQuantile(0.75))
          .prctl95((long) sizeSketch.getQuantile(0.95))
          .prctl99((long) sizeSketch.getQuantile(0.99))
          .prctl999((long) sizeSketch.getQuantile(0.999));
    }
  }

  static class HourlyCounts {

    // hour start ms -> count
    private final Map<Long, Long> hourlyStats = new HashMap<>();
    private final long minTs = Instant.now().minus(Duration.ofDays(14)).toEpochMilli();

    void apply(ConsumerRecord<?, ?> rec) {
      if (rec.timestamp() > minTs) {
        var hourStart = rec.timestamp() - rec.timestamp() % (1_000 * 60 * 60);
        hourlyStats.compute(hourStart, (h, cnt) -> cnt == null ? 1 : cnt + 1);
      }
    }

    void merge(HourlyCounts other) {
      // counts collected by the source are already filtered by its own minTs, so no cut-off is re-applied here
      other.hourlyStats.forEach((hourStart, cnt) -> hourlyStats.merge(hourStart, cnt, Long::sum));
    }

    List<TopicAnalysisStatsHourlyMsgCountsInnerDTO> toDto() {
      return hourlyStats.entrySet().stream()
          .sorted(Comparator.comparingLong(Map.Entry::getKey))
          .map(e -> new TopicAnalysisStatsHourlyMsgCountsInnerDTO()
              .hourStart(e.getKey())
              .count(e.getValue()))
          .collect(Collectors.toList());
    }
  }

  /**
   * Aggregates per-partition stats into topic-wide stats. Sketches are combined with their unions instead of
   * being fed the same records twice: quantile sketches are probabilistic, so two sketches built from identical
   * input can still disagree - most visibly on sparse tails like the 99.9th percentile.
   */
  static TopicAnalysisStats merge(Collection<TopicAnalysisStats> stats) {
    var merged = new TopicAnalysisStats();
    stats.forEach(merged::merge);
    return merged;
  }

  private void merge(TopicAnalysisStats other) {
    totalMsgs += other.totalMsgs;
    minOffset = minNullable(minOffset, other.minOffset);
    maxOffset = maxNullable(maxOffset, other.maxOffset);
    minTimestamp = minNullable(minTimestamp, other.minTimestamp);
    maxTimestamp = maxNullable(maxTimestamp, other.maxTimestamp);
    nullKeys += other.nullKeys;
    nullValues += other.nullValues;
    keysSize.merge(other.keysSize);
    valuesSize.merge(other.valuesSize);
    uniqKeys = union(uniqKeys, other.uniqKeys);
    uniqValues = union(uniqValues, other.uniqValues);
    hourlyCounts.merge(other.hourlyCounts);
  }

  void apply(ConsumerRecord<Bytes, Bytes> rec) {
    totalMsgs++;
    minTimestamp = minNullable(minTimestamp, rec.timestamp());
    maxTimestamp = maxNullable(maxTimestamp, rec.timestamp());
    minOffset = minNullable(minOffset, rec.offset());
    maxOffset = maxNullable(maxOffset, rec.offset());
    hourlyCounts.apply(rec);

    if (rec.key() != null) {
      byte[] keyBytes = rec.key().get();
      keysSize.apply(rec.serializedKeySize());
      uniqKeys.update(keyBytes);
    } else {
      nullKeys++;
    }

    if (rec.value() != null) {
      byte[] valueBytes = rec.value().get();
      valuesSize.apply(rec.serializedValueSize());
      uniqValues.update(valueBytes);
    } else {
      nullValues++;
    }
  }

  TopicAnalysisStatsDTO toDto(@Nullable Integer partition) {
    return new TopicAnalysisStatsDTO()
        .partition(partition)
        .totalMsgs(totalMsgs)
        .minOffset(minOffset)
        .maxOffset(maxOffset)
        .minTimestamp(minTimestamp)
        .maxTimestamp(maxTimestamp)
        .nullKeys(nullKeys)
        .nullValues(nullValues)
        // because of hll error estimated size can be greater that actual msgs count
        .approxUniqKeys(Math.min(totalMsgs, (long) uniqKeys.getEstimate()))
        .approxUniqValues(Math.min(totalMsgs, (long) uniqValues.getEstimate()))
        .keySize(keysSize.toDto())
        .valueSize(valuesSize.toDto())
        .hourlyMsgCounts(hourlyCounts.toDto());
  }

  private static HllSketch union(HllSketch s1, HllSketch s2) {
    var union = new Union(HllSketch.DEFAULT_LG_K);
    union.update(s1);
    union.update(s2);
    return union.getResult();
  }

  private static Long maxNullable(@Nullable Long v1, long v2) {
    return v1 == null ? v2 : Math.max(v1, v2);
  }

  // partitions that were never written to contribute no bounds at all
  private static Long maxNullable(@Nullable Long v1, @Nullable Long v2) {
    return v2 == null ? v1 : maxNullable(v1, (long) v2);
  }

  private static Long minNullable(@Nullable Long v1, long v2) {
    return v1 == null ? v2 : Math.min(v1, v2);
  }

  private static Long minNullable(@Nullable Long v1, @Nullable Long v2) {
    return v2 == null ? v1 : minNullable(v1, (long) v2);
  }
}
