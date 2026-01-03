package io.kafbat.ui.controller;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.service.ClustersStatisticsScheduler;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class PrometheusExposeControllerTest extends AbstractIntegrationTest {
  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private ClustersStatisticsScheduler scheduler;

  @Test
  void testGetMetrics() {

    scheduler.updateStatistics();

    webTestClient
        .get()
        .uri("/metrics")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(PrometheusTextFormatWriter.CONTENT_TYPE);
  }
}
