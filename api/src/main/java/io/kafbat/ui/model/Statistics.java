package io.kafbat.ui.model;

import io.kafbat.ui.api.model.ControllerType;
import io.kafbat.ui.service.ReactiveAdminClient;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Value;
import org.apache.kafka.clients.admin.TopicDescription;

@Value
@Builder(toBuilder = true)
public class Statistics implements AutoCloseable {
  ServerStatusDTO status;
  Throwable lastKafkaException;
  String version;
  List<ClusterFeature> features;
  ReactiveAdminClient.ClusterDescription clusterDescription;
  Metrics metrics;
  ScrapedClusterState clusterState;
  Map<String, KafkaConnectState> connectStates;
  ControllerType controller;
  InternalQuorumInfo quorumInfo;

  public static Statistics empty() {
    return builder()
        .status(ServerStatusDTO.OFFLINE)
        .version("Unknown")
        .features(List.of())
        .clusterDescription(ReactiveAdminClient.ClusterDescription.empty())
        .metrics(Metrics.empty())
        .clusterState(ScrapedClusterState.empty())
        .connectStates(Map.of())
        .build();
  }

  public static Statistics statsUpdateError(Throwable th) {
    return empty().toBuilder().status(ServerStatusDTO.OFFLINE).lastKafkaException(th).build();
  }

  public static Statistics initializing() {
    return empty().toBuilder().status(ServerStatusDTO.INITIALIZING).build();
  }

  public Stream<TopicDescription> topicDescriptions() {
    return clusterState.getTopicStates().values().stream().map(ScrapedClusterState.TopicState::description);
  }

  public Statistics withClusterState(UnaryOperator<ScrapedClusterState> stateUpdate) {
    return toBuilder().clusterState(stateUpdate.apply(clusterState)).build();
  }

  @Override
  public void close() throws Exception {
    if (clusterState != null) {
      clusterState.close();
    }
  }
}
