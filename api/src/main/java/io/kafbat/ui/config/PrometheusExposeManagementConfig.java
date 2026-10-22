package io.kafbat.ui.config;

import io.kafbat.ui.controller.PrometheusExposeController;
import io.kafbat.ui.service.StatisticsCache;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
@ConditionalOnProperty(value = PrometheusExposeManagementConfig.ENABLED_PROPERTY, havingValue = "true")
public class PrometheusExposeManagementConfig {

  public static final String ENABLED_PROPERTY = "metrics.management-port.enabled";

  @Bean
  PrometheusExposeController prometheusExposeController(StatisticsCache statisticsCache) {
    return new PrometheusExposeController(statisticsCache);
  }
}
