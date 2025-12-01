package io.kafbat.ui.mapper;

import io.kafbat.ui.api.model.ConsumerGroupLag;
import io.kafbat.ui.api.model.ConsumerGroupState;
import io.kafbat.ui.model.BrokerDTO;
import io.kafbat.ui.model.ConsumerGroupDTO;
import io.kafbat.ui.model.ConsumerGroupDetailsDTO;
import io.kafbat.ui.model.ConsumerGroupLagDTO;
import io.kafbat.ui.model.ConsumerGroupStateDTO;
import io.kafbat.ui.model.ConsumerGroupTopicPartitionDTO;
import io.kafbat.ui.model.InternalConsumerGroup;
import io.kafbat.ui.model.InternalTopicConsumerGroup;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;

public class ConsumerGroupMapper {

  private ConsumerGroupMapper() {
  }

  public static ConsumerGroupDTO toDto(InternalConsumerGroup c) {
    return convertToConsumerGroup(c, new ConsumerGroupDTO());
  }

  public static ConsumerGroupDTO toDto(InternalTopicConsumerGroup c) {
    ConsumerGroupDTO consumerGroup = new ConsumerGroupDetailsDTO();
    consumerGroup.setTopics(1); //for ui backward-compatibility, need to rm usage from ui
    consumerGroup.setGroupId(c.getGroupId());
    consumerGroup.setMembers(c.getMembers());
    consumerGroup.setConsumerLag(c.getConsumerLag());
    consumerGroup.setSimple(c.isSimple());
    consumerGroup.setPartitionAssignor(c.getPartitionAssignor());
    consumerGroup.setState(mapConsumerGroupState(c.getState()));
    Optional.ofNullable(c.getCoordinator())
        .ifPresent(cd -> consumerGroup.setCoordinator(mapCoordinator(cd)));
    return consumerGroup;
  }

  public static ConsumerGroupDetailsDTO toDetailsDto(InternalConsumerGroup g) {
    ConsumerGroupDetailsDTO details = convertToConsumerGroup(g, new ConsumerGroupDetailsDTO());
    Map<TopicPartition, ConsumerGroupTopicPartitionDTO> partitionMap = new HashMap<>();

    for (Map.Entry<TopicPartition, Long> entry : g.getOffsets().entrySet()) {
      ConsumerGroupTopicPartitionDTO partition = new ConsumerGroupTopicPartitionDTO();
      partition.setTopic(entry.getKey().topic());
      partition.setPartition(entry.getKey().partition());
      partition.setCurrentOffset(entry.getValue());

      final Optional<Long> endOffset = Optional.ofNullable(g.getEndOffsets())
          .map(o -> o.get(entry.getKey()));

      final Long behind = endOffset.map(o -> o - entry.getValue())
          .orElse(0L);

      partition.setEndOffset(endOffset.orElse(0L));
      partition.setConsumerLag(behind);

      partitionMap.put(entry.getKey(), partition);
    }

    for (InternalConsumerGroup.InternalMember member : g.getMembers()) {
      for (TopicPartition topicPartition : member.getAssignment()) {
        final ConsumerGroupTopicPartitionDTO partition = partitionMap.computeIfAbsent(
            topicPartition,
            tp -> new ConsumerGroupTopicPartitionDTO()
                .topic(tp.topic())
                .partition(tp.partition())
        );
        partition.setHost(member.getHost());
        partition.setConsumerId(member.getConsumerId());
        partitionMap.put(topicPartition, partition);
      }
    }
    details.setPartitions(new ArrayList<>(partitionMap.values()));
    return details;
  }

  public static ConsumerGroupLagDTO toDto(ScrapedClusterState.ConsumerGroupState state) {

    Set<TopicPartition> topicPartitions = Stream.concat(
        state.description().members().stream()
            .flatMap(m -> m.assignment().topicPartitions().stream()),
        state.committedOffsets().keySet().stream()
    ).collect(Collectors.toSet());

  }

  private static <T extends ConsumerGroupDTO> T convertToConsumerGroup(
      InternalConsumerGroup c, T consumerGroup) {
    consumerGroup.setGroupId(c.getGroupId());
    consumerGroup.setMembers(c.getMembers().size());
    consumerGroup.setConsumerLag(c.getConsumerLag());
    consumerGroup.setTopics(c.getTopicNum());
    consumerGroup.setSimple(c.isSimple());

    Optional.ofNullable(c.getState())
        .ifPresent(s -> consumerGroup.setState(mapConsumerGroupState(s)));
    Optional.ofNullable(c.getCoordinator())
        .ifPresent(cd -> consumerGroup.setCoordinator(mapCoordinator(cd)));

    consumerGroup.setPartitionAssignor(c.getPartitionAssignor());
    return consumerGroup;
  }

  private static BrokerDTO mapCoordinator(Node node) {
    return new BrokerDTO().host(node.host()).id(node.id()).port(node.port());
  }

  private static ConsumerGroupStateDTO mapConsumerGroupState(org.apache.kafka.common.ConsumerGroupState state) {
    return switch (state) {
      case DEAD -> ConsumerGroupStateDTO.DEAD;
      case EMPTY -> ConsumerGroupStateDTO.EMPTY;
      case STABLE -> ConsumerGroupStateDTO.STABLE;
      case PREPARING_REBALANCE -> ConsumerGroupStateDTO.PREPARING_REBALANCE;
      case COMPLETING_REBALANCE -> ConsumerGroupStateDTO.COMPLETING_REBALANCE;
      default -> ConsumerGroupStateDTO.UNKNOWN;
    };
  }

}
