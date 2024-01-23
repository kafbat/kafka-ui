package io.kafbat.ui.service;

import io.kafbat.ui.model.KafkaCluster;
import reactor.core.publisher.Mono;

public interface AdminClientService {

  Mono<ReactiveAdminClient> get(KafkaCluster cluster);

}
