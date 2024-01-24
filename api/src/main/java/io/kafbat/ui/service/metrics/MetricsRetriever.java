package io.kafbat.ui.service.metrics;

import io.kafbat.ui.model.KafkaCluster;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Flux;

interface MetricsRetriever {
  Flux<RawMetric> retrieve(KafkaCluster c, Node node);
}
