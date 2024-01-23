package io.kafbat.ui.service;

import io.kafbat.ui.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class BrokerServiceTest extends AbstractIntegrationTest {

  @Autowired
  private BrokerService brokerService;

  @Autowired
  private ClustersStorage clustersStorage;

  @Test
  void getBrokersReturnsFilledBrokerDto() {
    var localCluster = clustersStorage.getClusterByName(LOCAL).get();
    StepVerifier.create(brokerService.getBrokers(localCluster))
        .expectNextMatches(b -> b.getId().equals(1)
            && b.getHost().equals(kafka.getHost())
            && b.getPort().equals(kafka.getFirstMappedPort()))
        .verifyComplete();
  }

}
