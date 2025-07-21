package io.kafbat.ui.service.metrics.scrape.prometheus;

import io.kafbat.ui.model.MetricsScrapeProperties;
import io.kafbat.ui.util.WebClientConfigurator;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
class PrometheusMetricsRetriever {

  private static final String METRICS_ENDPOINT_PATH = "/metrics";
  private static final int DEFAULT_EXPORTER_PORT = 11001;

  private final int port;
  private final boolean sslEnabled;
  private final WebClient webClient;

  PrometheusMetricsRetriever(MetricsScrapeProperties scrapeProperties) {
    this.port = Optional.ofNullable(scrapeProperties.getPort()).orElse(DEFAULT_EXPORTER_PORT);
    this.sslEnabled = scrapeProperties.isSsl() || scrapeProperties.getKeystoreConfig() != null;
    this.webClient = new WebClientConfigurator()
        .configureBufferSize(DataSize.ofMegabytes(20))
        .configureBasicAuth(scrapeProperties.getUsername(), scrapeProperties.getPassword())
        .configureSsl(scrapeProperties.getTruststoreConfig(), scrapeProperties.getKeystoreConfig())
        .build();
  }

  Mono<List<MetricSnapshot>> retrieve(String host) {
    log.debug("Retrieving metrics from prometheus endpoint: {}:{}", host, port);

    var uri = UriComponentsBuilder.newInstance()
        .scheme(sslEnabled ? "https" : "http")
        .host(host)
        .port(port)
        .path(METRICS_ENDPOINT_PATH)
        .build()
        .toUri();

    return webClient.get()
        .uri(uri)
        .retrieve()
        .bodyToMono(String.class)
        .doOnError(e -> log.error("Error while getting metrics from {}", host, e))
        .map(body -> new PrometheusTextFormatParser().parse(body))
        .onErrorResume(th -> Mono.just(List.of()));
  }
}
