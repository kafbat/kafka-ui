package io.kafbat.ui.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

class PrometheusExposeManagementPortTest {

  private static final String MANAGEMENT_PORT = "management.server.port=0";
  private static final String CLUSTER_NAME = "kafka.clusters[0].name=local";
  private static final String CLUSTER_SERVERS = "kafka.clusters[0].bootstrapServers=localhost:1";

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
      properties = {MANAGEMENT_PORT, CLUSTER_NAME, CLUSTER_SERVERS})
  class ByDefault {

    @LocalServerPort
    int serverPort;

    @LocalManagementPort
    int managementPort;

    @Test
    void metricsStayOnMainPort() {
      client(serverPort).get().uri("/metrics/local").exchange().expectStatus().isOk();
      client(managementPort).get().uri("/metrics/local").exchange().expectStatus().isNotFound();
    }
  }

  @Nested
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
      properties = {MANAGEMENT_PORT, CLUSTER_NAME, CLUSTER_SERVERS, "metrics.management-port.enabled=true"})
  class Enabled {

    @LocalServerPort
    int serverPort;

    @LocalManagementPort
    int managementPort;

    @Test
    void metricsAreServedOnManagementPortOnly() {
      client(managementPort).get().uri("/metrics").exchange().expectStatus().isOk();
      client(managementPort).get().uri("/metrics/local").exchange().expectStatus().isOk();
      client(serverPort).get().uri("/metrics").exchange().expectStatus().isNotFound();
      client(serverPort).get().uri("/metrics/local").exchange().expectStatus().isNotFound();
    }
  }

  private static WebTestClient client(int port) {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }
}
