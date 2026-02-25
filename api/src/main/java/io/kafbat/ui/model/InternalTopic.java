package io.kafbat.ui.model;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.kafbat.ui.util.annotation.CsvIgnore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;

@Data
@Builder(toBuilder = true)
public class InternalTopic {

  // from TopicDescription
  private final String name;
  private final boolean internal;
  private final int replicas;
  private final int partitionCount;
  private final int inSyncReplicas;
  private final int replicationFactor;
  private final int underReplicatedPartitions;
  @CsvIgnore
  private final Map<Integer, InternalPartition> partitions;

  // topic configs
  @CsvIgnore
  private final List<InternalTopicConfig> topicConfigs;
  private final CleanupPolicy cleanUpPolicy;

  // rates from metrics
  private final BigDecimal bytesInPerSec;
  private final BigDecimal bytesOutPerSec;

  // from log dir data
  private final long segmentSize;
  private final long segmentCount;


  public InternalTopic withMetrics(Metrics metrics) {
    var builder = toBuilder();
    if (metrics != null) {
      builder.bytesInPerSec(metrics.getIoRates().topicBytesInPerSec().get(this.name));
      builder.bytesOutPerSec(metrics.getIoRates().topicBytesOutPerSec().get(this.name));
    }
    return builder.build();
  }

  public static InternalTopic from(TopicDescription topicDescription,
                                   List<ConfigEntry> configs,
                                   InternalPartitionsOffsets partitionsOffsets,
                                   Metrics metrics,
                                   @Nullable InternalLogDirStats.SegmentStats segmentStats,
                                   @Nullable Map<Integer, InternalLogDirStats.SegmentStats> partitionsSegmentStats,
                                   @Nullable String internalTopicPrefix) {
    var topic = InternalTopic.builder();

    internalTopicPrefix = internalTopicPrefix == null || internalTopicPrefix.isEmpty()
        ? "_"
        : internalTopicPrefix;

    topic.internal(
        topicDescription.isInternal() || topicDescription.name().startsWith(internalTopicPrefix)
    );
    topic.name(topicDescription.name());

    List<InternalPartition> partitions = topicDescription.partitions().stream()
        .map(partition -> {
          var partitionDto = InternalPartition.builder();

          partitionDto.leader(partition.leader() != null ? partition.leader().id() : null);
          partitionDto.partition(partition.partition());
          partitionDto.inSyncReplicasCount(partition.isr().size());
          partitionDto.replicasCount(partition.replicas().size());
          List<InternalReplica> replicas = partition.replicas().stream()
              .map(r ->
                  InternalReplica.builder()
                      .broker(r.id())
                      .inSync(partition.isr().contains(r))
                      .leader(partition.leader() != null && partition.leader().id() == r.id())
                      .build())
              .collect(Collectors.toList());
          partitionDto.replicas(replicas);

          partitionsOffsets.get(topicDescription.name(), partition.partition())
              .ifPresent(offsets -> {
                partitionDto.offsetMin(offsets.getEarliest());
                partitionDto.offsetMax(offsets.getLatest());
              });

          Optional.ofNullable(partitionsSegmentStats)
              .flatMap(s -> Optional.ofNullable(s.get(partition.partition())))
              .ifPresent(stats -> {
                partitionDto.segmentCount(stats.getSegmentsCount());
                partitionDto.segmentSize(stats.getSegmentSize());
              });


          return partitionDto.build();
        })
        .toList();

    topic.partitions(partitions.stream().collect(
        Collectors.toMap(InternalPartition::getPartition, t -> t)));

    var partitionsStats = new PartitionsStats(topicDescription);
    topic.replicas(partitionsStats.getReplicasCount());
    topic.partitionCount(partitionsStats.getPartitionsCount());
    topic.inSyncReplicas(partitionsStats.getInSyncReplicasCount());
    topic.underReplicatedPartitions(partitionsStats.getUnderReplicatedPartitionCount());

    topic.replicationFactor(
        topicDescription.partitions().isEmpty()
            ? 0
            : topicDescription.partitions().get(0).replicas().size()
    );

    Optional.ofNullable(segmentStats)
        .ifPresent(stats -> {
          topic.segmentCount(stats.getSegmentsCount());
          topic.segmentSize(stats.getSegmentSize());
        });

    if (metrics != null) {
      topic.bytesInPerSec(metrics.getIoRates().topicBytesInPerSec().get(topicDescription.name()));
      topic.bytesOutPerSec(metrics.getIoRates().topicBytesOutPerSec().get(topicDescription.name()));
    }

    topic.topicConfigs(
        configs.stream().map(InternalTopicConfig::from).collect(Collectors.toList()));

    topic.cleanUpPolicy(
        configs.stream()
            .filter(config -> config.name().equals(CLEANUP_POLICY_CONFIG))
            .findFirst()
            .map(ConfigEntry::value)
            .map(CleanupPolicy::fromString)
            .orElse(CleanupPolicy.UNKNOWN)
    );

    return topic.build();
  }

  public static InternalTopic from(ScrapedClusterState.TopicState topicState,
                                   @Nullable String internalTopicPrefix) {
    Map<TopicPartition, InternalPartitionsOffsets.Offsets> offsets =
        topicState.description().partitions().stream().map(p -> Map.entry(
            new TopicPartition(topicState.name(), p.partition()),
            new InternalPartitionsOffsets.Offsets(
                topicState.startOffsets().get(p.partition()),
                topicState.endOffsets().get(p.partition())
            )
        )
    ).filter(e ->
            e.getValue().getEarliest() != null && e.getValue().getLatest() != null
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return from(
        topicState.description(),
        topicState.configs(),
        new InternalPartitionsOffsets(offsets),
        null,
        topicState.segmentStats(),
        topicState.partitionsSegmentStats(),
        internalTopicPrefix
    );
  }

  public @Nullable Long getMessagesCount() {
    Long result = null;
    if (cleanUpPolicy.equals(CleanupPolicy.DELETE)) {
      result = 0L;
      if (partitions != null && !partitions.isEmpty()) {
        for (InternalPartition partition : partitions.values()) {
          if (partition.getOffsetMin() != null && partition.getOffsetMax() != null) {
            result += (partition.getOffsetMax() - partition.getOffsetMin());
          }
        }
      }
    }
    return result;
  }
}
