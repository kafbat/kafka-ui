package io.kafbat.ui.mapper;

import static io.kafbat.ui.util.MetricsUtils.readPointValue;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.BrokerConfigDTO;
import io.kafbat.ui.model.BrokerDTO;
import io.kafbat.ui.model.BrokerMetricsDTO;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ClusterFeature;
import io.kafbat.ui.model.ClusterMetricsDTO;
import io.kafbat.ui.model.ClusterStatsDTO;
import io.kafbat.ui.model.ConfigSourceDTO;
import io.kafbat.ui.model.ConfigSynonymDTO;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.InternalBroker;
import io.kafbat.ui.model.InternalBrokerConfig;
import io.kafbat.ui.model.InternalClusterState;
import io.kafbat.ui.model.InternalPartition;
import io.kafbat.ui.model.InternalReplica;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.InternalTopicConfig;
import io.kafbat.ui.model.KafkaAclDTO;
import io.kafbat.ui.model.KafkaAclNamePatternTypeDTO;
import io.kafbat.ui.model.KafkaAclResourceTypeDTO;
import io.kafbat.ui.model.MetricDTO;
import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.model.PartitionDTO;
import io.kafbat.ui.model.ReplicaDTO;
import io.kafbat.ui.model.TopicConfigDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.model.TopicDetailsDTO;
import io.kafbat.ui.model.TopicProducerStateDTO;
import io.kafbat.ui.service.metrics.SummarizedMetrics;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ProducerState;
import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;

@Mapper(componentModel = "spring")
public interface ClusterMapper {

  @Mapping(target = "defaultCluster", ignore = true)
  ClusterDTO toCluster(InternalClusterState clusterState);

  @Mapping(target = "zooKeeperStatus", ignore = true)
  ClusterStatsDTO toClusterStats(InternalClusterState clusterState);

  default ClusterMetricsDTO toClusterMetrics(Metrics metrics) {
    return new ClusterMetricsDTO()
        .items(convert(new SummarizedMetrics(metrics).asStream()).toList());
  }

  private Stream<MetricDTO> convert(Stream<MetricSnapshot> metrics) {
    return metrics
        .flatMap(m ->
            m.getDataPoints().stream()
                .map(p ->
                        new MetricDTO()
                            .name(m.getMetadata().getName())
                            .labels(p.getLabels().stream().collect(toMap(Label::getName, Label::getValue)))
                            .value(BigDecimal.valueOf(readPointValue(p)))
                )
        );
  }

  default BrokerMetricsDTO toBrokerMetrics(List<MetricSnapshot> metrics) {
    return new BrokerMetricsDTO().metrics(convert(metrics.stream()).toList());
  }

  @Mapping(target = "isSensitive", source = "sensitive")
  @Mapping(target = "isReadOnly", source = "readOnly")
  BrokerConfigDTO toBrokerConfig(InternalBrokerConfig config);

  default ConfigSynonymDTO toConfigSynonym(ConfigEntry.ConfigSynonym config) {
    if (config == null) {
      return null;
    }

    ConfigSynonymDTO configSynonym = new ConfigSynonymDTO();
    configSynonym.setName(config.name());
    configSynonym.setValue(config.value());
    if (config.source() != null) {
      configSynonym.setSource(ConfigSourceDTO.valueOf(config.source().name()));
    }

    return configSynonym;
  }

  TopicDTO toTopic(InternalTopic topic);

  default <T> JsonNullable<T> toJsonNullable(T value) {
    if (value == null) {
      return JsonNullable.undefined();
    } else {
      return JsonNullable.of(value);
    }
  }

  PartitionDTO toPartition(InternalPartition topic);

  BrokerDTO toBrokerDto(InternalBroker broker);

  @Mapping(target = "keySerde", ignore = true)
  @Mapping(target = "valueSerde", ignore = true)
  TopicDetailsDTO toTopicDetails(InternalTopic topic);

  @Mapping(target = "isReadOnly", source = "readOnly")
  @Mapping(target = "isSensitive", source = "sensitive")
  TopicConfigDTO toTopicConfig(InternalTopicConfig topic);

  ReplicaDTO toReplica(InternalReplica replica);

  @Mapping(target = "connectorsCount", ignore = true)
  @Mapping(target = "failedConnectorsCount", ignore = true)
  @Mapping(target = "tasksCount", ignore = true)
  @Mapping(target = "failedTasksCount", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "commit", ignore = true)
  @Mapping(target = "clusterId", ignore = true)
  ConnectDTO toKafkaConnect(ClustersProperties.ConnectCluster connect);

  List<ClusterDTO.FeaturesEnum> toFeaturesEnum(List<ClusterFeature> features);

  default List<PartitionDTO> map(Map<Integer, InternalPartition> map) {
    return map.values().stream().map(this::toPartition).collect(toList());
  }


  default TopicProducerStateDTO map(int partition, ProducerState state) {
    return new TopicProducerStateDTO()
        .partition(partition)
        .producerId(state.producerId())
        .producerEpoch(state.producerEpoch())
        .lastSequence(state.lastSequence())
        .lastTimestampMs(state.lastTimestamp())
        .coordinatorEpoch(state.coordinatorEpoch().stream().boxed().findAny().orElse(null))
        .currentTransactionStartOffset(state.currentTransactionStartOffset().stream().boxed().findAny().orElse(null));
  }

  static KafkaAclDTO.OperationEnum mapAclOperation(AclOperation operation) {
    return switch (operation) {
      case ALL -> KafkaAclDTO.OperationEnum.ALL;
      case READ -> KafkaAclDTO.OperationEnum.READ;
      case WRITE -> KafkaAclDTO.OperationEnum.WRITE;
      case CREATE -> KafkaAclDTO.OperationEnum.CREATE;
      case DELETE -> KafkaAclDTO.OperationEnum.DELETE;
      case ALTER -> KafkaAclDTO.OperationEnum.ALTER;
      case DESCRIBE -> KafkaAclDTO.OperationEnum.DESCRIBE;
      case CLUSTER_ACTION -> KafkaAclDTO.OperationEnum.CLUSTER_ACTION;
      case DESCRIBE_CONFIGS -> KafkaAclDTO.OperationEnum.DESCRIBE_CONFIGS;
      case ALTER_CONFIGS -> KafkaAclDTO.OperationEnum.ALTER_CONFIGS;
      case IDEMPOTENT_WRITE -> KafkaAclDTO.OperationEnum.IDEMPOTENT_WRITE;
      case CREATE_TOKENS -> KafkaAclDTO.OperationEnum.CREATE_TOKENS;
      case DESCRIBE_TOKENS -> KafkaAclDTO.OperationEnum.DESCRIBE_TOKENS;
      case ANY -> throw new IllegalArgumentException("ANY operation can be only part of filter");
      case UNKNOWN -> KafkaAclDTO.OperationEnum.UNKNOWN;
    };
  }

  static KafkaAclResourceTypeDTO mapAclResourceType(ResourceType resourceType) {
    return switch (resourceType) {
      case CLUSTER -> KafkaAclResourceTypeDTO.CLUSTER;
      case TOPIC -> KafkaAclResourceTypeDTO.TOPIC;
      case GROUP -> KafkaAclResourceTypeDTO.GROUP;
      case DELEGATION_TOKEN -> KafkaAclResourceTypeDTO.DELEGATION_TOKEN;
      case TRANSACTIONAL_ID -> KafkaAclResourceTypeDTO.TRANSACTIONAL_ID;
      case USER -> KafkaAclResourceTypeDTO.USER;
      case ANY -> throw new IllegalArgumentException("ANY type can be only part of filter");
      case UNKNOWN -> KafkaAclResourceTypeDTO.UNKNOWN;
    };
  }

  static ResourceType mapAclResourceTypeDto(KafkaAclResourceTypeDTO dto) {
    return ResourceType.valueOf(dto.name());
  }

  static PatternType mapPatternTypeDto(KafkaAclNamePatternTypeDTO dto) {
    return PatternType.valueOf(dto.name());
  }

  static AclBinding toAclBinding(KafkaAclDTO dto) {
    return new AclBinding(
        new ResourcePattern(
            mapAclResourceTypeDto(dto.getResourceType()),
            dto.getResourceName(),
            mapPatternTypeDto(dto.getNamePatternType())
        ),
        new AccessControlEntry(
            dto.getPrincipal(),
            dto.getHost(),
            AclOperation.valueOf(dto.getOperation().name()),
            AclPermissionType.valueOf(dto.getPermission().name())
        )
    );
  }

  static KafkaAclDTO toKafkaAclDto(AclBinding binding) {
    var pattern = binding.pattern();
    var filter = binding.toFilter().entryFilter();
    return new KafkaAclDTO()
        .resourceType(mapAclResourceType(pattern.resourceType()))
        .resourceName(pattern.name())
        .namePatternType(KafkaAclNamePatternTypeDTO.fromValue(pattern.patternType().name()))
        .principal(filter.principal())
        .host(filter.host())
        .operation(mapAclOperation(filter.operation()))
        .permission(KafkaAclDTO.PermissionEnum.fromValue(filter.permissionType().name()));
  }

}
