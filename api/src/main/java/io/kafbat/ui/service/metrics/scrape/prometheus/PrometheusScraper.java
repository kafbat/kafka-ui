package io.kafbat.ui.service.metrics.scrape.prometheus;

import io.kafbat.ui.model.MetricsScrapeProperties;
import io.kafbat.ui.service.metrics.scrape.BrokerMetricsScraper;
import io.kafbat.ui.service.metrics.scrape.PerBrokerScrapedMetrics;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class PrometheusScraper implements BrokerMetricsScraper {

  private final PrometheusMetricsRetriever retriever;

  public PrometheusScraper(MetricsScrapeProperties scrapeProperties) {
    this.retriever = new PrometheusMetricsRetriever(scrapeProperties);
  }

  @Override
  public Mono<PerBrokerScrapedMetrics> scrape(Collection<Node> clusterNodes) {
    Mono<Map<Integer, List<MetricSnapshot>>> collected = Flux.fromIterable(clusterNodes)
        .flatMap(n -> retriever.retrieve(n.host()).map(metrics -> Tuples.of(n, metrics)))
        .collectMap(t -> t.getT1().id(), Tuple2::getT2);
    return collected.map(PerBrokerScrapedMetrics::new);
  }
}
