package io.kafbat.ui.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the wiring of {@link KafkaCluster#getBootstrapServers()} into
 * {@link InternalClusterState} so the value reaches the cluster DTO.
 */
class InternalClusterStateTest {

  /** Configured bootstrap servers reach the InternalClusterState. */
  @Test
  void exposesBootstrapServersFromCluster() {
    KafkaCluster cluster = KafkaCluster.builder()
        .name("test-cluster")
        .bootstrapServers("host1:9092,host2:9092")
        .build();

    InternalClusterState state = new InternalClusterState(cluster, Statistics.empty());

    assertThat(state.getBootstrapServers()).isEqualTo("host1:9092,host2:9092");
  }

  /** Bootstrap servers stay null when none were configured. */
  @Test
  void bootstrapServersIsNullWhenNotConfigured() {
    KafkaCluster cluster = KafkaCluster.builder()
        .name("test-cluster")
        .build();

    InternalClusterState state = new InternalClusterState(cluster, Statistics.empty());

    assertThat(state.getBootstrapServers()).isNull();
  }
}
