package io.kafbat.ui.service.metrics.scrape;

import java.util.Collection;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Mono;

public interface BrokerMetricsScraper {

  Mono<PerBrokerScrapedMetrics> scrape(Collection<Node> clusterNodes);

}
