package io.kafbat.ui.service.metrics.scrape;

import static io.kafbat.ui.config.ClustersProperties.Cluster;
import static io.kafbat.ui.model.MetricsScrapeProperties.JMX_METRICS_TYPE;
import static io.kafbat.ui.model.MetricsScrapeProperties.PROMETHEUS_METRICS_TYPE;

import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.MetricsScrapeProperties;
import io.kafbat.ui.service.metrics.prometheus.PrometheusExpose;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsRetriever;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.prometheus.PrometheusScraper;
import io.kafbat.ui.service.metrics.sink.MetricsSink;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class MetricsScrapping {

  private final String clusterName;
  private final MetricsSink sink;
  private final InferredMetricsScraper inferredMetricsScraper;
  @Nullable
  private final JmxMetricsScraper jmxMetricsScraper;
  @Nullable
  private final PrometheusScraper prometheusScraper;

  public static MetricsScrapping create(Cluster cluster,
                                        JmxMetricsRetriever jmxMetricsRetriever) {
    JmxMetricsScraper jmxMetricsScraper = null;
    PrometheusScraper prometheusScraper = null;
    var metrics = cluster.getMetrics();
    if (cluster.getMetrics() != null) {
      var scrapeProperties = MetricsScrapeProperties.create(cluster);
      if (metrics.getType().equalsIgnoreCase(JMX_METRICS_TYPE) && metrics.getPort() != null) {
        jmxMetricsScraper = new JmxMetricsScraper(scrapeProperties, jmxMetricsRetriever);
      } else if (metrics.getType().equalsIgnoreCase(PROMETHEUS_METRICS_TYPE)) {
        prometheusScraper = new PrometheusScraper(scrapeProperties);
      }
    }
    return new MetricsScrapping(
        cluster.getName(),
        MetricsSink.create(cluster),
        new InferredMetricsScraper(),
        jmxMetricsScraper,
        prometheusScraper
    );
  }

  public Mono<Metrics> scrape(ScrapedClusterState clusterState, Collection<Node> nodes) {
    Mono<InferredMetrics> inferred = inferredMetricsScraper.scrape(clusterState);
    Mono<PerBrokerScrapedMetrics> external = scrapeExternal(nodes);
    return inferred.zipWith(
        external,
        (inf, ext) -> Metrics.builder()
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

  private Stream<MetricSnapshot> prepareMetricsForSending(Metrics metrics) {
    return PrometheusExpose.prepareMetricsForGlobalExpose(clusterName, metrics);
  }

  private Mono<PerBrokerScrapedMetrics> scrapeExternal(Collection<Node> nodes) {
    if (jmxMetricsScraper != null) {
      return jmxMetricsScraper.scrape(nodes);
    }
    if (prometheusScraper != null) {
      return prometheusScraper.scrape(nodes);
    }
    return Mono.just(PerBrokerScrapedMetrics.empty());
  }

}
