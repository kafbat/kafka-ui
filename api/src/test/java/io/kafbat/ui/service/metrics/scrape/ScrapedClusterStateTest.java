package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableTable;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.service.ReactiveAdminClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ScrapedClusterStateTest {

  @Test
  void emptyStateHasNonNullTopicIndex() throws Exception {
    try (ScrapedClusterState empty = ScrapedClusterState.empty()) {
      assertThat(empty.getTopicIndex()).isNotNull();
      assertThat(empty.getTopicIndex().find(null, null, false, null)).isEmpty();
      assertThat(empty.getTopicIndex().find("search", true, true, null)).isEmpty();
    }
  }

  @Test
  void scrapeDegradesGracefullyWhenListOffsetsTimesOut() throws Exception {
    // given - a cluster (e.g. Confluent Cloud) where listOffsets() times out, as reported in
    // https://github.com/kafbat/kafka-ui/issues/1852
    var topicName = "test-topic";
    ReactiveAdminClient.ClusterDescription clusterDescription =
        new ReactiveAdminClient.ClusterDescription(null, "test-cluster", List.of(), Set.of());

    TopicDescription topicDescription = new TopicDescription(
        topicName, false, List.of(new TopicPartitionInfo(0, null, List.of(), List.of())));

    ReactiveAdminClient ac = Mockito.mock(ReactiveAdminClient.class);
    Mockito.when(ac.describeLogDirs(List.of())).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listConsumerGroups()).thenReturn(Mono.just(List.of()));
    Mockito.when(ac.describeTopics()).thenReturn(Mono.just(Map.of(topicName, topicDescription)));
    Mockito.when(ac.getTopicsConfig()).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listOffsets(ArgumentMatchers.any(), ArgumentMatchers.any(OffsetSpec.class)))
        .thenReturn(Mono.error(new TimeoutException("Timed out waiting for offsets")));
    Mockito.when(ac.describeConsumerGroups(List.of())).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listConsumerGroupOffsets(List.of(), null))
        .thenReturn(Mono.just(ImmutableTable.of()));

    // when - then: the whole scrape must still complete (not error out) and keep the topic,
    // simply without offset info, rather than failing entirely and losing topic/broker info too
    try (ScrapedClusterState result =
        ScrapedClusterState.scrape(clusterDescription, ac, new ClustersProperties()).block()) {
      assertThat(result).isNotNull();
      var topicState = result.getTopicStates().get(topicName);
      assertThat(topicState).isNotNull();
      assertThat(topicState.startOffsets()).isEmpty();
      assertThat(topicState.endOffsets()).isEmpty();
    }
  }
}
