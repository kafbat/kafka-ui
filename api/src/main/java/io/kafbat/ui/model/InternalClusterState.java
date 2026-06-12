package io.kafbat.ui.model;

import com.google.common.base.Throwables;
import io.kafbat.ui.api.model.ControllerType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.kafka.common.Node;
import org.jetbrains.annotations.Nullable;

@Data
public class InternalClusterState {
  private String name;
  private ServerStatusDTO status;
  private MetricsCollectionErrorDTO lastError;
  private Integer topicCount;
  private Integer brokerCount;
  private Integer activeControllers;
  private Integer onlinePartitionCount;
  private Integer offlinePartitionCount;
  private Integer inSyncReplicasCount;
  private Integer outOfSyncReplicasCount;
  private Integer underReplicatedPartitionCount;
  private List<BrokerDiskUsageDTO> diskUsage;
  private String version;
  private List<ClusterFeature> features;
  private BigDecimal bytesInPerSec;
  private BigDecimal bytesOutPerSec;
  private Boolean readOnly;
  private ControllerType controller;

  public InternalClusterState(KafkaCluster cluster, Statistics statistics) {
    name = cluster.getName();
    status = statistics.getStatus();
    lastError = Optional.ofNullable(statistics.getLastKafkaException())
        .map(e -> new MetricsCollectionErrorDTO()
            .message(e.getMessage())
            .stackTrace(Throwables.getStackTraceAsString(e)))
        .orElse(null);
    topicCount = (int) statistics.topicDescriptions().count();
    brokerCount = statistics.getClusterDescription().getNodes().size();
    activeControllers = getActiveControllers(statistics);
    version = statistics.getVersion();

    diskUsage = statistics.getClusterState().getNodesStates().values().stream()
        .filter(n -> n.segmentStats() != null)
        .map(n -> new BrokerDiskUsageDTO()
            .brokerId(n.id())
            .segmentSize(n.segmentStats().getSegmentSize())
            .segmentCount(n.segmentStats().getSegmentsCount()))
        .collect(Collectors.toList());

    features = statistics.getFeatures();

    var ioRates = statistics.getMetrics().getIoRates();
    bytesInPerSec = sumWithTopicFallback(ioRates.brokerBytesInPerSec(), ioRates.topicBytesInPerSec());
    bytesOutPerSec = sumWithTopicFallback(ioRates.brokerBytesOutPerSec(), ioRates.topicBytesOutPerSec());

    var partitionsStats = new PartitionsStats(statistics.topicDescriptions().toList());
    onlinePartitionCount = partitionsStats.getOnlinePartitionCount();
    offlinePartitionCount = partitionsStats.getOfflinePartitionCount();
    inSyncReplicasCount = partitionsStats.getInSyncReplicasCount();
    outOfSyncReplicasCount = partitionsStats.getOutOfSyncReplicasCount();
    underReplicatedPartitionCount = partitionsStats.getUnderReplicatedPartitionCount();
    readOnly = cluster.isReadOnly();
    controller = statistics.getController();
  }

  /**
   * Aggregates a cluster-wide IO rate from the per-broker rates.
   *
   * <p>Some brokers do not expose the topic-less {@code BrokerTopicMetrics} aggregate over JMX
   * (observed with Confluent {@code cp-kafka}), so the per-broker map ends up empty even though
   * per-topic rates are scraped successfully. Without a fallback the cluster/broker throughput is
   * reported as {@code null} ("0 bytes") while every topic still shows a non-zero rate. In that
   * case we fall back to summing the per-topic rates, which by definition equals the all-topics
   * broker aggregate (bytes in/out are additive across topics, counted once at the leader broker).
   */
  @Nullable
  static BigDecimal sumWithTopicFallback(Map<Integer, BigDecimal> brokerRates,
                                         Map<String, BigDecimal> topicRates) {
    return brokerRates.values().stream()
        .reduce(BigDecimal::add)
        .or(() -> topicRates.values().stream().reduce(BigDecimal::add))
        .orElse(null);
  }

  @Nullable
  private static Integer getActiveControllers(Statistics statistics) {
    if (ControllerType.KRAFT == statistics.getController()) {
      return statistics.getQuorumInfo().leaderId();
    }

    return Optional.ofNullable(statistics.getClusterDescription().getController())
        .map(Node::id)
        .orElse(null);
  }

}
