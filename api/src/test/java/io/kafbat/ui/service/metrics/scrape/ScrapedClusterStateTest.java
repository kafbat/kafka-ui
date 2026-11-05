package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableTable;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.service.ReactiveAdminClient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
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
  void scrapeDegradesGracefullyWhenListConsumerGroupsIsNotAuthorized() {
    // given - a cluster (e.g. Confluent Cloud) that denies listConsumerGroups() with a
    // ClusterAuthorizationException, as reported in
    // https://github.com/kafbat/kafka-ui/issues/1852
    ReactiveAdminClient.ClusterDescription clusterDescription =
        new ReactiveAdminClient.ClusterDescription(null, "test-cluster", List.of(), Set.of());

    ReactiveAdminClient ac = Mockito.mock(ReactiveAdminClient.class);
    Mockito.when(ac.describeLogDirs(List.of())).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listConsumerGroups())
        .thenReturn(Mono.error(
            new ClusterAuthorizationException("Not authorized to list consumer groups")));
    Mockito.when(ac.describeTopics()).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.getTopicsConfig()).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listOffsets(ArgumentMatchers.any(), ArgumentMatchers.eq(OffsetSpec.latest())))
        .thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listOffsets(ArgumentMatchers.any(), ArgumentMatchers.eq(OffsetSpec.earliest())))
        .thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.describeConsumerGroups(List.of())).thenReturn(Mono.just(Map.of()));
    Mockito.when(ac.listConsumerGroupOffsets(List.of(), null))
        .thenReturn(Mono.just(ImmutableTable.of()));

    // when - then: the whole scrape must still complete (not error out) and simply have no
    // consumer group info, rather than failing entirely and losing topic/broker info too
    try (ScrapedClusterState result =
        ScrapedClusterState.scrape(clusterDescription, ac, new ClustersProperties()).block()) {
      assertThat(result).isNotNull();
      assertThat(result.getConsumerGroupsStates()).isEmpty();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
