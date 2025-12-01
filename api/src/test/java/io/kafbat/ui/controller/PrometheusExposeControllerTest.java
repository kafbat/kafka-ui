package io.kafbat.ui.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.service.ClustersStatisticsScheduler;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class PrometheusExposeControllerTest extends AbstractIntegrationTest {
  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private ClustersStatisticsScheduler scheduler;

  @Test
  void testGetMetrics() throws IOException {

    scheduler.updateStatistics();

    webTestClient
        .get()
        .uri("/metrics")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(PrometheusTextFormatWriter.CONTENT_TYPE);
  }
}
