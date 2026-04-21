package io.kafbat.ui.controller;

import io.kafbat.ui.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class KsqlControllerDisableViewingTest extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void executeKsqlReturns403WhenMessageViewingDisabled() {
    webTestClient
        .post()
        .uri("/api/clusters/{clusterName}/ksql/v2", MESSAGE_VIEWING_DISABLED_LOCAL)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"ksql\":\"SHOW STREAMS;\",\"streamsProperties\":{}}")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message").isEqualTo("Message viewing is disabled for this cluster");
  }

  @Test
  void openKsqlResponsePipeReturns403WhenMessageViewingDisabled() {
    webTestClient
        .get()
        .uri("/api/clusters/{clusterName}/ksql/response?pipeId=fake-pipe-id",
            MESSAGE_VIEWING_DISABLED_LOCAL)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message").isEqualTo("Message viewing is disabled for this cluster");
  }

}
