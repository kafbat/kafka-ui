package io.kafbat.ui.service;

import static io.kafbat.ui.service.ReactiveAdminClient.toMonoWithExceptionFilter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.InvalidMetadataException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactiveAdminClientExceptionFilterTest {

  @Test
  void suppressesExactConfiguredExceptionAndPreservesSuccessfulResults() {
    Map<String, KafkaFuture<String>> results = Map.of(
        "failed", failedFuture(new UnknownTopicOrPartitionException()),
        "successful", KafkaFuture.completedFuture("value")
    );

    StepVerifier.create(toMonoWithExceptionFilter(results, UnknownTopicOrPartitionException.class))
        .assertNext(result -> assertThat(result).containsExactlyEntriesOf(Map.of("successful", "value")))
        .verifyComplete();
  }

  @Test
  void suppressesSubclassOfConfiguredException() {
    StepVerifier.create(toMonoWithExceptionFilter(
        Map.of("failed", failedFuture(new UnknownTopicOrPartitionException())),
        InvalidMetadataException.class))
        .assertNext(result -> assertThat(result).isEmpty())
        .verifyComplete();
  }

  @Test
  void propagatesSuperclassOfConfiguredException() {
    var error = new KafkaException("unexpected Kafka failure");

    StepVerifier.create(toMonoWithExceptionFilter(
        Map.of("failed", failedFuture(error)), UnknownTopicOrPartitionException.class))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  @Test
  void propagatesUnrelatedException() {
    var error = new TimeoutException("request timed out");

    StepVerifier.create(toMonoWithExceptionFilter(
        Map.of("failed", failedFuture(error)), UnknownTopicOrPartitionException.class))
        .expectErrorMatches(actual -> actual == error)
        .verify();
  }

  private static <T> KafkaFuture<T> failedFuture(Throwable error) {
    var future = new KafkaFutureImpl<T>();
    future.completeExceptionally(error);
    return future;
  }
}
