package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.config.ClustersProperties.Cluster;
import static io.kafbat.ui.model.MetricsScrapeProperties.JMX_METRICS_TYPE;
import static io.kafbat.ui.model.MetricsScrapeProperties.PROMETHEUS_METRICS_TYPE;

import io.kafbat.ui.config.ClustersProperties.MetricsConfig;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.MetricsScrapeProperties;
import io.kafbat.ui.service.metrics.prometheus.PrometheusMetricsExposer;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsRetriever;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.prometheus.PrometheusScraper;
import io.kafbat.ui.service.metrics.sink.MetricsSink;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import jakarta.annotation.Nullable;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MetricsScraper {

  private final String clusterName;
  private final MetricsSink sink;
  private final InferredMetricsScraper inferredMetricsScraper;
  @Nullable
  private final BrokerMetricsScraper brokerMetricsScraper;

  public static MetricsScraper create(Cluster cluster,
                                      JmxMetricsRetriever jmxMetricsRetriever) {
    BrokerMetricsScraper scraper = null;
    MetricsConfig metricsConfig = cluster.getMetrics();
    if (cluster.getMetrics() != null) {
      var scrapeProperties = MetricsScrapeProperties.create(cluster);
      if (metricsConfig.getType().equalsIgnoreCase(JMX_METRICS_TYPE) && metricsConfig.getPort() != null) {
        scraper = new JmxMetricsScraper(scrapeProperties, jmxMetricsRetriever);
      } else if (metricsConfig.getType().equalsIgnoreCase(PROMETHEUS_METRICS_TYPE)) {
        scraper = new PrometheusScraper(scrapeProperties);
      }
    }
    return new MetricsScraper(
        cluster.getName(),
        MetricsSink.create(cluster),
        new InferredMetricsScraper(),
        scraper
    );
  }

  public Mono<Metrics> scrape(ScrapedClusterState clusterState, Collection<Node> nodes) {
    Mono<InferredMetrics> inferred = inferredMetricsScraper.scrape(clusterState);
    Mono<PerBrokerScrapedMetrics> brokerMetrics = scrapeBrokers(nodes);
    return inferred.zipWith(
        brokerMetrics,
        (inf, ext) ->
            Metrics.builder()
                .inferredMetrics(inf)
                .ioRates(ext.ioRates())
                .perBrokerScrapedMetrics(ext.perBrokerMetrics())
                .build()
    ).doOnNext(this::sendMetricsToSink);
  }

  private void sendMetricsToSink(Metrics metrics) {
    sink.send(prepareMetricsForSending(metrics))
        .doOnError(th -> log.warn("Error sending metrics to metrics sink", th))
        .subscribe();
  }

  private Flux<MetricSnapshot> prepareMetricsForSending(Metrics metrics) {
    //need to be "cold" because sinks can resubscribe multiple times
    return Flux.defer(() ->
        Flux.fromStream(
            PrometheusMetricsExposer.prepareMetricsForGlobalExpose(clusterName, metrics)));
  }

  private Mono<PerBrokerScrapedMetrics> scrapeBrokers(Collection<Node> nodes) {
    if (brokerMetricsScraper != null) {
      return brokerMetricsScraper.scrape(nodes);
    }
    return Mono.just(PerBrokerScrapedMetrics.empty());
  }

}
