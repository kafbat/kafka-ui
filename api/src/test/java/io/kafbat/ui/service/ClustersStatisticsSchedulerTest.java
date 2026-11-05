package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.Statistics;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ClustersStatisticsSchedulerTest {

  @Test
  void oneFailingClusterDoesNotBlockOthers() {
    KafkaCluster ok1 = KafkaCluster.builder().name("ok-1").build();
    KafkaCluster broken = KafkaCluster.builder().name("broken").build();
    KafkaCluster ok2 = KafkaCluster.builder().name("ok-2").build();

    ClustersStorage storage = mock(ClustersStorage.class);
    when(storage.getKafkaClusters()).thenReturn(List.of(ok1, broken, ok2));

    ConcurrentMap<String, Boolean> updated = new ConcurrentHashMap<>();
    StatisticsService statisticsService = mock(StatisticsService.class);
    when(statisticsService.updateCache(any())).thenAnswer(inv -> {
      KafkaCluster c = inv.getArgument(0);
      if (c.getName().equals(broken.getName())) {
        return Mono.error(new IllegalStateException("boom for " + c.getName()));
      }
      return Mono.just(Statistics.empty())
          .doOnSuccess(ignored -> updated.put(c.getName(), true));
    });

    new ClustersStatisticsScheduler(storage, statisticsService).updateStatistics();

    assertThat(updated).containsOnlyKeys(ok1.getName(), ok2.getName());
  }
}
