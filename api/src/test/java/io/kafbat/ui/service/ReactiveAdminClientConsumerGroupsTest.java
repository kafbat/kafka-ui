package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveAdminClientConsumerGroupsTest {

  private static final String ALLOWED = "application-group";
  private static final String DENIED = "restricted-group";
  private static final TopicPartition PARTITION = new TopicPartition("events", 0);

  private final AdminClient admin = mock(AdminClient.class);
  private final ClustersProperties.AdminClient properties = new ClustersProperties.AdminClient();
  private final ReactiveAdminClient client = new ReactiveAdminClient(admin, Mono.empty(), properties, null);

  @Test
  void describeSkipsUnauthorizedGroupsWithoutSplittingTheRequest() {
    var description = mock(ConsumerGroupDescription.class);
    var result = mock(DescribeConsumerGroupsResult.class);
    var groups = List.of(ALLOWED, DENIED);
    when(admin.describeConsumerGroups(groups)).thenReturn(result);
    when(result.describedGroups()).thenReturn(Map.of(
        ALLOWED, KafkaFuture.completedFuture(description),
        DENIED, failedFuture(new GroupAuthorizationException(DENIED))
    ));

    StepVerifier.create(client.describeConsumerGroups(groups, true))
        .assertNext(descriptions -> assertThat(descriptions).containsOnlyKeys(ALLOWED)
            .containsEntry(ALLOWED, description))
        .verifyComplete();

    verify(admin).describeConsumerGroups(groups);
    verifyNoMoreInteractions(admin);
  }

  @Test
  void describeReturnsEmptyMapWhenEveryGroupIsUnauthorized() {
    var result = mock(DescribeConsumerGroupsResult.class);
    when(admin.describeConsumerGroups(List.of(DENIED))).thenReturn(result);
    when(result.describedGroups()).thenReturn(Map.of(
        DENIED, failedFuture(new GroupAuthorizationException(DENIED))
    ));

    StepVerifier.create(client.describeConsumerGroups(List.of(DENIED), true))
        .assertNext(descriptions -> assertThat(descriptions).isEmpty())
        .verifyComplete();
  }

  @Test
  void describePreservesBatchSizeAndMergesAuthorizedResults() {
    properties.setDescribeConsumerGroupsPartitionSize(2);
    var description = mock(ConsumerGroupDescription.class);
    when(admin.describeConsumerGroups(any())).thenAnswer(invocation -> {
      Collection<String> groups = invocation.getArgument(0);
      var result = mock(DescribeConsumerGroupsResult.class);
      when(result.describedGroups()).thenReturn(groups.stream().collect(java.util.stream.Collectors.toMap(
          group -> group,
          group -> group.equals(DENIED)
              ? failedFuture(new GroupAuthorizationException(group))
              : KafkaFuture.completedFuture(description)
      )));
      return result;
    });

    StepVerifier.create(client.describeConsumerGroups(List.of(ALLOWED, DENIED, "another-group"), true))
        .assertNext(descriptions -> assertThat(descriptions).containsOnlyKeys(ALLOWED, "another-group"))
        .verifyComplete();

    verify(admin).describeConsumerGroups(List.of(ALLOWED, DENIED));
    verify(admin).describeConsumerGroups(List.of("another-group"));
    verifyNoMoreInteractions(admin);
  }

  @Test
  void offsetsSkipUnauthorizedGroupsAndPreservePartitionSelection() {
    var result = mock(ListConsumerGroupOffsetsResult.class);
    when(admin.listConsumerGroupOffsets(anyMap())).thenReturn(result);
    when(result.partitionsToOffsetAndMetadata(ALLOWED))
        .thenReturn(KafkaFuture.completedFuture(Map.of(PARTITION, new OffsetAndMetadata(42L))));
    when(result.partitionsToOffsetAndMetadata(DENIED))
        .thenReturn(failedFuture(new GroupAuthorizationException(DENIED)));

    StepVerifier.create(client.listConsumerGroupOffsets(List.of(ALLOWED, DENIED), List.of(PARTITION), true))
        .assertNext(offsets -> {
          assertThat(offsets.rowKeySet()).containsExactly(ALLOWED);
          assertThat(offsets.get(ALLOWED, PARTITION)).isEqualTo(42L);
        })
        .verifyComplete();

    ArgumentCaptor<Map<String, ListConsumerGroupOffsetsSpec>> requests = ArgumentCaptor.captor();
    verify(admin).listConsumerGroupOffsets(requests.capture());
    assertThat(requests.getValue()).containsOnlyKeys(ALLOWED, DENIED);
    requests.getValue().values().forEach(spec -> assertThat(spec.topicPartitions())
        .containsExactly(PARTITION));
    verifyNoMoreInteractions(admin);
  }

  @Test
  void offsetsReturnEmptyTableWhenEveryGroupIsUnauthorized() {
    var result = mock(ListConsumerGroupOffsetsResult.class);
    when(admin.listConsumerGroupOffsets(anyMap())).thenReturn(result);
    when(result.partitionsToOffsetAndMetadata(DENIED))
        .thenReturn(failedFuture(new GroupAuthorizationException(DENIED)));

    StepVerifier.create(client.listConsumerGroupOffsets(List.of(DENIED), null, true))
        .assertNext(offsets -> assertThat(offsets.isEmpty()).isTrue())
        .verifyComplete();
  }

  @ParameterizedTest
  @MethodSource("unrelatedFailures")
  void describePropagatesOtherErrors(Throwable error) {
    var result = mock(DescribeConsumerGroupsResult.class);
    when(admin.describeConsumerGroups(List.of(ALLOWED))).thenReturn(result);
    when(result.describedGroups()).thenReturn(Map.of(ALLOWED, failedFuture(error)));

    StepVerifier.create(client.describeConsumerGroups(List.of(ALLOWED), true))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  @ParameterizedTest
  @MethodSource("unrelatedFailures")
  void offsetsPropagateOtherErrors(Throwable error) {
    var result = mock(ListConsumerGroupOffsetsResult.class);
    when(admin.listConsumerGroupOffsets(anyMap())).thenReturn(result);
    when(result.partitionsToOffsetAndMetadata(ALLOWED)).thenReturn(failedFuture(error));

    StepVerifier.create(client.listConsumerGroupOffsets(List.of(ALLOWED), null, true))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  @Test
  void strictDescribePreservesAuthorizationFailure() {
    var error = new GroupAuthorizationException(DENIED);
    var result = mock(DescribeConsumerGroupsResult.class);
    when(admin.describeConsumerGroups(List.of(DENIED))).thenReturn(result);
    when(result.all()).thenReturn(failedFuture(error));

    StepVerifier.create(client.describeConsumerGroups(List.of(DENIED)))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  @Test
  void deletingOffsetsPreservesAuthorizationFailure() {
    var error = new GroupAuthorizationException(DENIED);
    var result = mock(ListConsumerGroupOffsetsResult.class);
    when(admin.listConsumerGroupOffsets(anyMap())).thenReturn(result);
    when(result.all()).thenReturn(failedFuture(error));

    StepVerifier.create(client.deleteConsumerGroupOffsets(DENIED, PARTITION.topic()))
        .expectErrorMatches(actual -> actual == error)
        .verify();

    verify(admin).listConsumerGroupOffsets(anyMap());
    verifyNoMoreInteractions(admin);
  }

  @Test
  void resettingOffsetsPreservesAuthorizationFailure() {
    var error = new GroupAuthorizationException(DENIED);
    var listings = mock(ListConsumerGroupsResult.class);
    when(admin.listConsumerGroups()).thenReturn(listings);
    when(listings.all()).thenReturn(KafkaFuture.completedFuture(List.of(new ConsumerGroupListing(DENIED, false))));
    var descriptions = mock(DescribeConsumerGroupsResult.class);
    when(admin.describeConsumerGroups(List.of(DENIED))).thenReturn(descriptions);
    when(descriptions.all()).thenReturn(failedFuture(error));
    var cluster = KafkaCluster.builder().name("test").build();
    var adminService = mock(AdminClientService.class);
    when(adminService.get(cluster)).thenReturn(Mono.just(client));

    StepVerifier.create(new OffsetsResetService(adminService).resetToLatest(cluster, DENIED, PARTITION.topic(), null))
        .expectErrorMatches(actual -> actual == error)
        .verify();

    verify(admin).listConsumerGroups();
    verify(admin).describeConsumerGroups(List.of(DENIED));
    verifyNoMoreInteractions(admin);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void clusterScrapeSucceedsWithUnauthorizedGroups(boolean allUnauthorized) throws Exception {
    var groups = List.of(ALLOWED, DENIED);
    var description = new ConsumerGroupDescription(ALLOWED, false, List.of(), "range",
        ConsumerGroupState.EMPTY, null);
    var descriptions = mock(DescribeConsumerGroupsResult.class);
    when(admin.describeConsumerGroups(groups)).thenReturn(descriptions);
    when(descriptions.describedGroups()).thenReturn(Map.of(
        ALLOWED, allUnauthorized ? failedFuture(new GroupAuthorizationException(ALLOWED))
            : KafkaFuture.completedFuture(description),
        DENIED, failedFuture(new GroupAuthorizationException(DENIED))
    ));
    var offsets = mock(ListConsumerGroupOffsetsResult.class);
    when(admin.listConsumerGroupOffsets(anyMap())).thenReturn(offsets);
    when(offsets.partitionsToOffsetAndMetadata(ALLOWED))
        .thenReturn(allUnauthorized ? failedFuture(new GroupAuthorizationException(ALLOWED))
            : KafkaFuture.completedFuture(Map.of(PARTITION, new OffsetAndMetadata(42L))));
    when(offsets.partitionsToOffsetAndMetadata(DENIED))
        .thenReturn(failedFuture(new GroupAuthorizationException(DENIED)));

    var scrapeClient = spy(client);
    doReturn(Mono.just(Map.of())).when(scrapeClient).describeLogDirs(any());
    doReturn(Mono.just(List.of(new ConsumerGroupListing(ALLOWED, false),
        new ConsumerGroupListing(DENIED, false)))).when(scrapeClient).listConsumerGroups();
    doReturn(Mono.just(Map.of(PARTITION.topic(), new TopicDescription(PARTITION.topic(), false, List.of()))))
        .when(scrapeClient).describeTopics();
    doReturn(Mono.just(Map.of())).when(scrapeClient).getTopicsConfig();
    doReturn(Mono.just(Map.of())).when(scrapeClient).listOffsets(anyCollection(), any());

    try (var state = ScrapedClusterState.scrape(ReactiveAdminClient.ClusterDescription.empty(),
        scrapeClient, new ClustersProperties()).block()) {
      assertThat(state).isNotNull();
      assertThat(state.getTopicStates()).containsOnlyKeys(PARTITION.topic());
      if (allUnauthorized) {
        assertThat(state.getConsumerGroupsStates()).isEmpty();
      } else {
        assertThat(state.getConsumerGroupsStates()).containsOnlyKeys(ALLOWED);
        assertThat(state.getConsumerGroupsStates().get(ALLOWED).committedOffsets()).containsEntry(PARTITION, 42L);
      }
    }
  }

  static Stream<Throwable> unrelatedFailures() {
    return Stream.of(new TimeoutException("timed out"), new SaslAuthenticationException("authentication failed"));
  }

  private static <T> KafkaFuture<T> failedFuture(Throwable error) {
    var future = new KafkaFutureImpl<T>();
    future.completeExceptionally(error);
    return future;
  }
}
