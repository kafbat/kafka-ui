package io.kafbat.ui.service;

import io.kafbat.ui.model.KafkaCluster;
import reactor.core.publisher.Mono;

public interface AdminClientService extends AutoCloseable {

  Mono<ReactiveAdminClient> get(KafkaCluster cluster);

  void invalidate(KafkaCluster cluster, Throwable e);

}
