package io.kafbat.ui.config;

import io.kafbat.ui.controller.PrometheusExposeController;
import io.kafbat.ui.service.StatisticsCache;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.context.annotation.Bean;

// Not @Configuration: component scan would also register it in the main context, it belongs to the management one.
@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
public class PrometheusExposeManagementConfig {

  @Bean
  PrometheusExposeController prometheusExposeController(StatisticsCache statisticsCache) {
    return new PrometheusExposeController(statisticsCache);
  }
}
