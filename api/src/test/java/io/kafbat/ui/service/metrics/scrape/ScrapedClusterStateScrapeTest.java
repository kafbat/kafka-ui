package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashBasedTable;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.ReactiveAdminClient.ClusterDescription;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;

class ScrapedClusterStateScrapeTest {

  private static final Node NODE = new Node(1, "node1", 9092);
  private static final ConfigEntry CLEANUP_DELETE = new ConfigEntry("cleanup.policy", "delete");
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  private final ClustersProperties properties = new ClustersProperties();
  private final ClusterDescription clusterDescription =
      new ClusterDescription(NODE, "cluster-id", List.of(NODE), Set.of());

  @Test
  void scrapeIssuesSingleListTopicsCall() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState scraped = scrape(ac, ScrapedClusterState.empty(), Duration.ZERO)) {
      assertThat(scraped.getTopicStates()).containsOnlyKeys("t1");
    }
    verify(ac, times(1)).listTopics(true);
  }

  @Test
  void scrapeSkipsConfigFetchOnSecondTickWithinExpiry() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState first = scrape(ac, ScrapedClusterState.empty(), ONE_HOUR)) {
      verify(ac, times(1)).getTopicsConfig(anyCollection(), anyBoolean());
      try (ScrapedClusterState second = scrape(ac, first, ONE_HOUR)) {
        assertThat(second.getTopicStates()).containsOnlyKeys("t1");
      }
    }
    // still exactly the one call from the first tick
    verify(ac, times(1)).getTopicsConfig(anyCollection(), anyBoolean());
  }

  @Test
  void scrapeCarriesTopicConfigsForwardOnSkippedTick() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState first = scrape(ac, ScrapedClusterState.empty(), ONE_HOUR);
         ScrapedClusterState second = scrape(ac, first, ONE_HOUR)) {
      assertThat(second.getTopicStates().get("t1").configs()).containsExactly(CLEANUP_DELETE);
      assertThat(second.getTopicConfigsRefreshedAt()).isEqualTo(first.getTopicConfigsRefreshedAt());
    }
  }

  @Test
  void scrapeRequestsConfigsOnlyForNewTopicsWithinExpiry() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState first = scrape(ac, ScrapedClusterState.empty(), ONE_HOUR)) {
      stubTopics(ac, Set.of("t1", "t2"));
      try (ScrapedClusterState second = scrape(ac, first, ONE_HOUR)) {
        assertThat(second.getTopicStates()).containsOnlyKeys("t1", "t2");
        // t1's configs came from the previous state, t2's from the incremental fetch
        assertThat(second.getTopicStates().get("t1").configs()).containsExactly(CLEANUP_DELETE);
        assertThat(second.getTopicStates().get("t2").configs()).containsExactly(CLEANUP_DELETE);
      }
    }

    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.captor();
    verify(ac, times(2)).getTopicsConfig(captor.capture(), eq(false));
    assertThat(captor.getAllValues().get(0)).containsExactly("t1");
    assertThat(captor.getAllValues().get(1)).containsExactly("t2");
  }

  @Test
  void scrapeMakesNoConfigCallAtAllWhenNothingIsStale() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState first = scrape(ac, ScrapedClusterState.empty(), ONE_HOUR)) {
      ReactiveAdminClient fresh = adminClient(Set.of("t1"));
      try (ScrapedClusterState second = scrape(fresh, first, ONE_HOUR)) {
        assertThat(second.getTopicStates().get("t1").configs()).containsExactly(CLEANUP_DELETE);
      }
      verify(fresh, never()).getTopicsConfig(anyCollection(), anyBoolean());
    }
  }

  @Test
  void scrapeRefetchesAllConfigsWhenExpiryIsZero() throws Exception {
    ReactiveAdminClient ac = adminClient(Set.of("t1"));
    try (ScrapedClusterState first = scrape(ac, ScrapedClusterState.empty(), Duration.ZERO);
         ScrapedClusterState second = scrape(ac, first, Duration.ZERO)) {
      assertThat(second.getTopicStates()).containsOnlyKeys("t1");
    }
    verify(ac, times(2)).getTopicsConfig(anyCollection(), anyBoolean());
  }

  private ScrapedClusterState scrape(ReactiveAdminClient ac, ScrapedClusterState previous, Duration expiry) {
    return ScrapedClusterState.scrape(clusterDescription, ac, properties, previous, expiry).block();
  }

  private static ReactiveAdminClient adminClient(Set<String> topics) {
    ReactiveAdminClient ac = mock(ReactiveAdminClient.class);
    // Mono.zip completes empty if any source is empty, so every method the scrape touches must be stubbed
    when(ac.describeLogDirs(anyList())).thenReturn(Mono.just(Map.of()));
    when(ac.listConsumerGroups()).thenReturn(Mono.just(List.of()));
    when(ac.describeConsumerGroups(anyCollection())).thenReturn(Mono.just(Map.of()));
    when(ac.listConsumerGroupOffsets(anyList(), isNull())).thenReturn(Mono.just(HashBasedTable.create()));
    // listOffsets is overloaded, so the matcher has to be typed to pick the TopicDescription overload
    when(ac.listOffsets(ArgumentMatchers.<Collection<TopicDescription>>any(), any()))
        .thenReturn(Mono.just(Map.of()));
    stubTopics(ac, topics);
    return ac;
  }

  private static void stubTopics(ReactiveAdminClient ac, Set<String> topics) {
    when(ac.listTopics(true)).thenReturn(Mono.just(topics));
    when(ac.describeTopics(anyCollection())).thenAnswer(invocation -> {
      Collection<String> requested = invocation.getArgument(0);
      return Mono.just(requested.stream().collect(Collectors.toMap(t -> t, ScrapedClusterStateScrapeTest::describe)));
    });
    when(ac.getTopicsConfig(anyCollection(), anyBoolean())).thenAnswer(invocation -> {
      Collection<String> requested = invocation.getArgument(0);
      return Mono.just(requested.stream().collect(Collectors.toMap(t -> t, t -> List.of(CLEANUP_DELETE))));
    });
  }

  private static TopicDescription describe(String topic) {
    return new TopicDescription(topic, false, List.of(new TopicPartitionInfo(0, NODE, List.of(NODE), List.of(NODE))));
  }
}
