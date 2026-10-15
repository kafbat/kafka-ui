package io.kafbat.ui.model;

import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Value;
import org.apache.kafka.common.TopicPartition;

@Builder
@Value
public class Metrics {

  IoRates ioRates;
  SegmentCounts segmentCounts;
  InferredMetrics inferredMetrics;
  Map<Integer, List<MetricSnapshot>> perBrokerScrapedMetrics;

  public static Metrics empty() {
    return Metrics.builder()
        .ioRates(IoRates.empty())
        .segmentCounts(SegmentCounts.empty())
        .perBrokerScrapedMetrics(Map.of())
        .inferredMetrics(InferredMetrics.empty())
        .build();
  }

  /**
   * Number of log segments, taken from the {@code NumLogSegments} broker metric.
   *
   * @param brokerSegmentCount    broker id -> total segments hosted by that broker (leaders and followers)
   * @param partitionSegmentCount topic-partition -> (broker id -> segments of that replica)
   */
  public record SegmentCounts(Map<Integer, Integer> brokerSegmentCount,
                              Map<TopicPartition, Map<Integer, Integer>> partitionSegmentCount) {

    public static SegmentCounts empty() {
      return new SegmentCounts(Map.of(), Map.of());
    }

    @Nullable
    public Integer forBroker(int brokerId) {
      return brokerSegmentCount.get(brokerId);
    }

    /**
     * Segment count of a partition. Replicas can differ slightly in segment count, so the leader's
     * value is used when known, otherwise the largest value reported by any replica.
     */
    @Nullable
    public Integer forPartition(TopicPartition partition, @Nullable Integer leaderId) {
      Map<Integer, Integer> perBroker = partitionSegmentCount.get(partition);
      if (perBroker == null || perBroker.isEmpty()) {
        return null;
      }
      if (leaderId != null && perBroker.containsKey(leaderId)) {
        return perBroker.get(leaderId);
      }
      return perBroker.values().stream().max(Comparator.naturalOrder()).orElse(null);
    }
  }

  @Builder
  public record IoRates(Map<Integer, BigDecimal> brokerBytesInPerSec,
                        Map<Integer, BigDecimal> brokerBytesOutPerSec,
                        Map<String, BigDecimal> topicBytesInPerSec,
                        Map<String, BigDecimal> topicBytesOutPerSec) {

    static IoRates empty() {
      return IoRates.builder()
          .brokerBytesOutPerSec(Map.of())
          .brokerBytesInPerSec(Map.of())
          .topicBytesOutPerSec(Map.of())
          .topicBytesInPerSec(Map.of())
          .build();
    }
  }

}
