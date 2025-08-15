package io.kafbat.ui.service.metrics.scrape.jmx;

import io.kafbat.ui.model.MetricsScrapeProperties;
import io.kafbat.ui.service.metrics.RawMetric;
import io.kafbat.ui.service.metrics.scrape.BrokerMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.PerBrokerScrapedMetrics;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

public class JmxMetricsScraper implements BrokerMetricsScraper {

  private final JmxMetricsRetriever jmxMetricsRetriever;
  private final MetricsScrapeProperties scrapeProperties;

  public JmxMetricsScraper(MetricsScrapeProperties scrapeProperties,
                           JmxMetricsRetriever jmxMetricsRetriever) {
    this.scrapeProperties = scrapeProperties;
    this.jmxMetricsRetriever = jmxMetricsRetriever;
  }

  @Override
  public Mono<PerBrokerScrapedMetrics> scrape(Collection<Node> nodes) {
    Mono<Map<Integer, List<MetricSnapshot>>> collected = Flux.fromIterable(nodes)
        .flatMap(n -> jmxMetricsRetriever.retrieveFromNode(scrapeProperties, n).map(metrics -> Tuples.of(n, metrics)))
        .collectMap(
            t -> t.getT1().id(),
            t -> RawMetric.groupIntoSnapshot(t.getT2()).toList()
        );
    return collected.map(PerBrokerScrapedMetrics::new);
  }
}
