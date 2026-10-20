package io.kafbat.ui.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=0"
)
class PrometheusExposeManagementPortTest {

  @LocalServerPort
  private int serverPort;

  @LocalManagementPort
  private int managementPort;

  @Test
  void metricsAreServedOnManagementPortOnly() {
    client(managementPort).get().uri("/metrics").exchange().expectStatus().isOk();
    client(serverPort).get().uri("/metrics").exchange().expectStatus().isNotFound();
  }

  private static WebTestClient client(int port) {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }
}
