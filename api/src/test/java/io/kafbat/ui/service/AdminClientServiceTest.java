package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.KafkaCluster;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;

@Slf4j
class AdminClientServiceTest extends AbstractIntegrationTest {
  @Test
  void testInvalidateOnFailure() {
    AdminClientService adminClientService = applicationContext.getBean(AdminClientService.class);
    ClustersStorage clustersStorage = applicationContext.getBean(ClustersStorage.class);
    KafkaCluster cluster = clustersStorage.getClusterByName(LOCAL).get();
    ReactiveAdminClient clientBefore = adminClientService.get(cluster).block();
    ReactiveAdminClient clientBeforeRepeat = adminClientService.get(cluster).block();
    assertThat(clientBeforeRepeat).isEqualTo(clientBefore);
    adminClientService.invalidate(cluster, new TimeoutException());
    ReactiveAdminClient clientAfter = adminClientService.get(cluster).block();
    assertThat(clientAfter).isNotEqualTo(clientBefore);
  }
}
