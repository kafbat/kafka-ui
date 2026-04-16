package io.kafbat.ui.controller;

import io.kafbat.ui.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class MessagesControllerDisableViewingTest extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void returns403WithoutBasicAuthPromptWhenMessageViewingDisabled() {
    webTestClient
        .get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/messages/v2?mode=LATEST&limit=1",
            MESSAGE_VIEWING_DISABLED_LOCAL, "__consumer_offsets")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .doesNotExist("WWW-Authenticate")
        .expectBody()
        .jsonPath("$.message").isEqualTo("Message viewing is disabled for this cluster");
  }

  @Test
  void serdesEndpointReturns403WhenMessageViewingDisabled() {
    webTestClient
        .get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/serdes?use=DESERIALIZE",
            MESSAGE_VIEWING_DISABLED_LOCAL, "__consumer_offsets")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .doesNotExist("WWW-Authenticate");
  }

  @Test
  void messagesStillWorkOnClusterWithoutFlag() {
    webTestClient
        .get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/serdes?use=DESERIALIZE",
            LOCAL, "__consumer_offsets")
        .exchange()
        .expectStatus()
        .isOk();
  }
}
