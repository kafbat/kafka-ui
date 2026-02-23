package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.ConsumerPosition;
import java.util.Comparator;
import java.util.TreeMap;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class RangePollingEmitterTest {

  @Test
  void nextChunkSizePerPartitionBoundedByPollCap() {
    // 30 partitions, page 30, pollCap 200 -> maxTotalRequested = 200, maxChunkByTotal = ceil(200/30) = 7
    PollingSettings settings = createPollingSettings(200);
    TestRangeEmitter emitter = new TestRangeEmitter(30, settings);
    int chunk = emitter.nextChunkSizePerPartitionPublic(30);
    assertThat(chunk).isLessThanOrEqualTo(7);
    assertThat(chunk).isPositive();
  }

  @Test
  void nextChunkSizePerPartitionManyPartitionsSmallPage() {
    // 50 partitions, page 20, pollCap 500 -> reduces tiny iterations
    PollingSettings settings = createPollingSettings(500);
    TestRangeEmitter emitter = new TestRangeEmitter(20, settings);
    int chunk = emitter.nextChunkSizePerPartitionPublic(50);
    assertThat(chunk * 50).isLessThanOrEqualTo(500);
    assertThat(chunk).isPositive();
  }

  @Test
  void nextChunkSizePerPartitionSinglePartition() {
    PollingSettings settings = createPollingSettings(500);
    TestRangeEmitter emitter = new TestRangeEmitter(30, settings);
    assertThat(emitter.nextChunkSizePerPartitionPublic(1)).isPositive();
  }

  private static PollingSettings createPollingSettings(int maxMessagesToScanPerPoll) {
    ClustersProperties.Cluster cluster = new ClustersProperties.Cluster();
    cluster.setName("test-cluster");
    ClustersProperties rootProps = new ClustersProperties();
    ClustersProperties.PollingProperties pollingProps = new ClustersProperties.PollingProperties();
    pollingProps.setMaxMessagesToScanPerPoll(maxMessagesToScanPerPoll);
    rootProps.setPolling(pollingProps);
    return PollingSettings.create(cluster, rootProps);
  }

  private static final class TestRangeEmitter extends RangePollingEmitter {

    TestRangeEmitter(int messagesPerPage, PollingSettings settings) {
      super(
          () -> null,
          mock(ConsumerPosition.class),
          messagesPerPage,
          mock(MessagesProcessing.class),
          settings,
          mock(Cursor.Tracking.class)
      );
    }

    @Override
    protected TreeMap<TopicPartition, FromToOffset> nextPollingRange(
        TreeMap<TopicPartition, FromToOffset> prevRange,
        SeekOperations seekOperations) {
      return new TreeMap<>(Comparator.comparingInt(TopicPartition::partition));
    }

    int nextChunkSizePerPartitionPublic(int activePartitions) {
      return nextChunkSizePerPartition(activePartitions);
    }
  }
}
