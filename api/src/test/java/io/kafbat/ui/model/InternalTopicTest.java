package io.kafbat.ui.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

class InternalTopicTest {

  private static final String TOPIC = "test";
  private static final Node NODE_1 = new Node(1, "host1", 9092);
  private static final Node NODE_2 = new Node(2, "host2", 9092);
  private static final Node NODE_3 = new Node(3, "host3", 9092);

  // 2 partitions, replication factor 3, leaders on brokers 1 and 2
  private static final TopicDescription DESCRIPTION = new TopicDescription(
      TOPIC,
      false,
      List.of(
          new TopicPartitionInfo(0, NODE_1, List.of(NODE_1, NODE_2, NODE_3), List.of(NODE_1, NODE_2, NODE_3)),
          new TopicPartitionInfo(1, NODE_2, List.of(NODE_1, NODE_2, NODE_3), List.of(NODE_1, NODE_2, NODE_3))
      )
  );

  private static final InternalLogDirStats.SegmentStats SEGMENT_STATS = new InternalLogDirStats.SegmentStats(300L);

  @Test
  void segmentCountTakenFromLeaderReplicaMetricsAndSummedPerTopic() {
    var metrics = metricsWith(new Metrics.SegmentCounts(
        Map.of(1, 7, 2, 5, 3, 4),
        Map.of(
            new TopicPartition(TOPIC, 0), Map.of(1, 4, 2, 3, 3, 3),
            new TopicPartition(TOPIC, 1), Map.of(1, 3, 2, 2, 3, 1)
        )));

    var topic = InternalTopic.from(DESCRIPTION, List.of(), InternalPartitionsOffsets.empty(), metrics,
        SEGMENT_STATS, Map.of(0, SEGMENT_STATS, 1, SEGMENT_STATS), "_");

    assertThat(topic.getPartitions().get(0).getSegmentCount()).isEqualTo(4);
    assertThat(topic.getPartitions().get(1).getSegmentCount()).isEqualTo(2);
    assertThat(topic.getSegmentCount()).isEqualTo(6);
    // size still comes from describeLogDirs and is not multiplied by the replica count
    assertThat(topic.getSegmentSize()).isEqualTo(300L);
    assertThat(topic.getPartitions().get(0).getSegmentSize()).isEqualTo(300L);
  }

  @Test
  void segmentCountIsNullWhenMetricsAreNotConfigured() {
    var topic = InternalTopic.from(DESCRIPTION, List.of(), InternalPartitionsOffsets.empty(), Metrics.empty(),
        SEGMENT_STATS, Map.of(0, SEGMENT_STATS, 1, SEGMENT_STATS), "_");

    assertThat(topic.getSegmentCount()).isNull();
    assertThat(topic.getPartitions().get(0).getSegmentCount()).isNull();
    assertThat(topic.getSegmentSize()).isEqualTo(300L);

    var withoutMetrics = InternalTopic.from(DESCRIPTION, List.of(), InternalPartitionsOffsets.empty(), null,
        SEGMENT_STATS, null, "_");
    assertThat(withoutMetrics.getSegmentCount()).isNull();
  }

  @Test
  void withMetricsFillsSegmentCountForIndexedTopics() {
    // topics built from the cluster state index have no metrics attached, they're added later via withMetrics
    var topic = InternalTopic.from(DESCRIPTION, List.of(), InternalPartitionsOffsets.empty(), null,
        SEGMENT_STATS, null, "_");
    assertThat(topic.getSegmentCount()).isNull();

    var metrics = metricsWith(new Metrics.SegmentCounts(
        Map.of(1, 2),
        Map.of(new TopicPartition(TOPIC, 0), Map.of(1, 2))));

    var withMetrics = topic.withMetrics(metrics);

    assertThat(withMetrics.getPartitions().get(0).getSegmentCount()).isEqualTo(2);
    assertThat(withMetrics.getPartitions().get(1).getSegmentCount()).isNull();
    assertThat(withMetrics.getSegmentCount()).isEqualTo(2);
  }

  private static Metrics metricsWith(Metrics.SegmentCounts segmentCounts) {
    var empty = Metrics.empty();
    return Metrics.builder()
        .ioRates(empty.getIoRates())
        .inferredMetrics(empty.getInferredMetrics())
        .perBrokerScrapedMetrics(empty.getPerBrokerScrapedMetrics())
        .segmentCounts(segmentCounts)
        .build();
  }
}
