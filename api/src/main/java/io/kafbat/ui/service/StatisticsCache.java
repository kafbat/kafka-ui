package io.kafbat.ui.service;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.InternalPartitionsOffsets;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.index.TopicsIndexRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StatisticsCache {

  private final Map<String, Statistics> cache = new ConcurrentHashMap<>();
  private final TopicsIndexRegistry topicsIndexRegistry;
  private final ClustersProperties clustersProperties;

  public StatisticsCache(ClustersStorage clustersStorage, TopicsIndexRegistry topicsIndexRegistry,
                         ClustersProperties clustersProperties) {
    Statistics initializing = Statistics.initializing();
    this.topicsIndexRegistry = topicsIndexRegistry;
    this.clustersProperties = clustersProperties;
    clustersStorage.getKafkaClusters().forEach(c -> cache.put(c.getName(), initializing));
  }

  public synchronized void replace(KafkaCluster c, Statistics stats) {
    cache.put(c.getName(), stats);
  }

  public synchronized void update(KafkaCluster c,
                                  Map<String, TopicDescription> descriptions,
                                  Map<String, List<ConfigEntry>> configs,
                                  InternalPartitionsOffsets partitionsOffsets) {
    var stats = get(c);
    var topicIndex = topicsIndexRegistry.get(c.getName());
    replace(
        c,
        stats.withClusterState(s ->
            s.updateTopics(descriptions, configs, partitionsOffsets, clustersProperties, topicIndex)
        )
    );
    try {
      if (!stats.getStatus().equals(ServerStatusDTO.INITIALIZING)) {
        stats.close();
      }
    } catch (Exception e) {
      log.error("Error closing cluster {} stats", c.getName(), e);
    }
  }

  public synchronized void onTopicDelete(KafkaCluster c, String topic) {
    var stats = get(c);
    var topicIndex = topicsIndexRegistry.get(c.getName());
    replace(
        c,
        stats.withClusterState(s -> s.topicDeleted(topic, clustersProperties, topicIndex))
    );
  }

  public Statistics get(KafkaCluster c) {
    return Objects.requireNonNull(cache.get(c.getName()), "Statistics for unknown cluster requested");
  }

}
