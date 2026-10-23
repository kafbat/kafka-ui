package io.kafbat.ui.service.metrics.scrape;

import static org.apache.commons.lang3.Strings.CI;

import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;

/**
 * Scans scraped broker metrics (JMX or Prometheus) for the {@code NumLogSegments} gauge that Kafka
 * exposes per partition replica ({@code kafka.log:type=Log,name=NumLogSegments,topic=...,partition=...}).
 *
 * <p>Depending on the scrape path the metric arrives under different names:
 * <ul>
 *   <li>JMX via {@code JmxMetricsFormatter}: {@code kafka_log_Log_Value{name="NumLogSegments",topic,partition}}</li>
 *   <li>Prometheus via jmx_exporter rules: {@code kafka_log_numlogsegments{topic,partition}} (name varies by
 *   rule set, but always contains "numlogsegments")</li>
 * </ul>
 * Both forms are accepted. The Admin API does not expose segment counts, so this is the only source for them.
 */
class SegmentCountMetricsScanner {

  static final String NUM_LOG_SEGMENTS = "NumLogSegments";
  static final String TOPIC_LABEL = "topic";
  static final String PARTITION_LABEL = "partition";
  static final String NAME_LABEL = "name";

  // topic-partition -> (broker id -> number of log segments of that replica)
  final Map<TopicPartition, Map<Integer, Integer>> partitionSegmentCount = new HashMap<>();

  SegmentCountMetricsScanner(Map<Integer, List<MetricSnapshot>> perBrokerMetrics) {
    for (Map.Entry<Integer, List<MetricSnapshot>> broker : perBrokerMetrics.entrySet()) {
      Integer nodeId = broker.getKey();
      for (MetricSnapshot metric : broker.getValue()) {
        String name = metric.getMetadata().getName();
        if (metric instanceof GaugeSnapshot gauge) {
          gauge.getDataPoints().forEach(dp -> update(nodeId, name, dp.getLabels(), dp.getValue()));
        } else if (metric instanceof UnknownSnapshot unknown) {
          unknown.getDataPoints().forEach(dp -> update(nodeId, name, dp.getLabels(), dp.getValue()));
        }
      }
    }
  }

  Metrics.SegmentCounts get() {
    Map<Integer, Integer> brokerSegmentCount = new HashMap<>();
    partitionSegmentCount.values().forEach(perBroker ->
        perBroker.forEach((brokerId, count) -> brokerSegmentCount.merge(brokerId, count, Integer::sum)));
    return new Metrics.SegmentCounts(brokerSegmentCount, partitionSegmentCount);
  }

  private void update(int nodeId, String name, Labels labels, double value) {
    if (!isNumLogSegments(name, labels)
        || !labels.contains(TOPIC_LABEL)
        || !labels.contains(PARTITION_LABEL)
        || !Double.isFinite(value)) {
      return;
    }
    int partition;
    try {
      partition = Integer.parseInt(labels.get(PARTITION_LABEL));
    } catch (NumberFormatException e) {
      return;
    }
    var tp = new TopicPartition(labels.get(TOPIC_LABEL), partition);
    partitionSegmentCount.computeIfAbsent(tp, k -> new HashMap<>()).put(nodeId, (int) value);
  }

  private static boolean isNumLogSegments(String name, Labels labels) {
    if (labels.contains(NAME_LABEL)) {
      return NUM_LOG_SEGMENTS.equalsIgnoreCase(labels.get(NAME_LABEL));
    }
    return CI.contains(name, NUM_LOG_SEGMENTS);
  }

}
