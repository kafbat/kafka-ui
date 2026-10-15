package io.kafbat.ui.service.metrics.scrape;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.service.metrics.scrape.prometheus.PrometheusTextFormatParser;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class SegmentCountMetricsScannerTest {

  private Metrics.SegmentCounts segmentCounts;

  @Test
  void jmxFormattedMetricsPopulated() {
    // name produced by JmxMetricsFormatter for kafka.log:type=Log,name=NumLogSegments,topic=...,partition=...
    populateWith(
        nodeMetrics(
            new Node(1, "host1", 123),
            "kafka_log_Log_Value{name=\"NumLogSegments\",topic=\"test\",partition=\"0\",} 4.0",
            "kafka_log_Log_Value{name=\"NumLogSegments\",topic=\"test\",partition=\"1\",} 2.0",
            "kafka_log_Log_Value{name=\"Size\",topic=\"test\",partition=\"0\",} 100500.0",
            "kafka_log_Log_Value{name=\"LogEndOffset\",topic=\"test\",partition=\"0\",} 77.0"
        ),
        nodeMetrics(
            new Node(2, "host2", 123),
            "kafka_log_Log_Value{name=\"NumLogSegments\",topic=\"test\",partition=\"0\",} 3.0",
            "kafka_log_Log_Value{name=\"NumLogSegments\",topic=\"other\",partition=\"0\",} 1.0"
        )
    );

    assertThat(segmentCounts.partitionSegmentCount())
        .containsEntry(new TopicPartition("test", 0), Map.of(1, 4, 2, 3))
        .containsEntry(new TopicPartition("test", 1), Map.of(1, 2))
        .containsEntry(new TopicPartition("other", 0), Map.of(2, 1))
        .hasSize(3);

    assertThat(segmentCounts.brokerSegmentCount())
        .containsEntry(1, 6)
        .containsEntry(2, 4)
        .hasSize(2);
  }

  @Test
  void prometheusExporterMetricsPopulated() {
    // names produced by common jmx_exporter rule sets (lowercased, no "name" label)
    populateWith(
        nodeMetrics(
            new Node(1, "host1", 123),
            "kafka_log_numlogsegments{partition=\"0\",topic=\"test\",} 5.0",
            "kafka_log_log_numlogsegments{partition=\"1\",topic=\"test\",} 2.0",
            "kafka_log_size{partition=\"0\",topic=\"test\",} 100500.0",
            "kafka_log_numlogsegments{partition=\"not_a_number\",topic=\"test\",} 1.0",
            "kafka_log_numlogsegments{topic=\"no_partition_label\",} 1.0",
            "kafka_log_numlogsegments{partition=\"0\",topic=\"nan\",} NaN"
        )
    );

    assertThat(segmentCounts.partitionSegmentCount())
        .containsEntry(new TopicPartition("test", 0), Map.of(1, 5))
        .containsEntry(new TopicPartition("test", 1), Map.of(1, 2))
        .hasSize(2);

    assertThat(segmentCounts.brokerSegmentCount())
        .containsEntry(1, 7)
        .hasSize(1);
  }

  @Test
  void emptyWhenNoSegmentMetrics() {
    populateWith(
        nodeMetrics(
            new Node(1, "host1", 123),
            "kafka_server_BrokerTopicMetrics_FifteenMinuteRate{name=\"BytesInPerSec\",topic=\"test\",} 1.0"
        )
    );

    assertThat(segmentCounts.partitionSegmentCount()).isEmpty();
    assertThat(segmentCounts.brokerSegmentCount()).isEmpty();
    assertThat(segmentCounts.forBroker(1)).isNull();
    assertThat(segmentCounts.forPartition(new TopicPartition("test", 0), 1)).isNull();
  }

  @Test
  void partitionCountPrefersLeaderReplica() {
    populateWith(
        nodeMetrics(
            new Node(1, "host1", 123),
            "kafka_log_numlogsegments{partition=\"0\",topic=\"test\",} 4.0"
        ),
        nodeMetrics(
            new Node(2, "host2", 123),
            "kafka_log_numlogsegments{partition=\"0\",topic=\"test\",} 3.0"
        )
    );

    var tp = new TopicPartition("test", 0);
    assertThat(segmentCounts.forPartition(tp, 2)).isEqualTo(3);
    assertThat(segmentCounts.forPartition(tp, 1)).isEqualTo(4);
    // leader unknown or has no metric: fall back to the largest replica value
    assertThat(segmentCounts.forPartition(tp, null)).isEqualTo(4);
    assertThat(segmentCounts.forPartition(tp, 99)).isEqualTo(4);
  }

  @SafeVarargs
  private void populateWith(Map.Entry<Integer, List<MetricSnapshot>>... entries) {
    segmentCounts = new SegmentCountMetricsScanner(
        stream(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
    ).get();
  }

  private Map.Entry<Integer, List<MetricSnapshot>> nodeMetrics(Node n, String... prometheusMetrics) {
    return Map.entry(n.id(), new PrometheusTextFormatParser().parse(String.join("\n", prometheusMetrics)));
  }

}
