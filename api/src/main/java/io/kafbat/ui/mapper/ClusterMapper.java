package io.kafbat.ui.mapper;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.BrokerConfigDTO;
import io.kafbat.ui.model.BrokerDTO;
import io.kafbat.ui.model.BrokerDiskUsageDTO;
import io.kafbat.ui.model.BrokerMetricsDTO;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ClusterFeature;
import io.kafbat.ui.model.ClusterMetricsDTO;
import io.kafbat.ui.model.ClusterStatsDTO;
import io.kafbat.ui.model.ConfigSourceDTO;
import io.kafbat.ui.model.ConfigSynonymDTO;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorStatusDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.model.InternalBroker;
import io.kafbat.ui.model.InternalBrokerConfig;
import io.kafbat.ui.model.InternalBrokerDiskUsage;
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
import io.kafbat.ui.model.TaskDTO;
import io.kafbat.ui.model.TopicConfigDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.model.TopicDetailsDTO;
import io.kafbat.ui.model.TopicProducerStateDTO;
import io.kafbat.ui.model.connect.InternalConnectorInfo;
import io.kafbat.ui.service.metrics.RawMetric;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

@Mapper(componentModel = "spring")
public interface ClusterMapper {

  @Mapping(target = "defaultCluster", ignore = true)
  ClusterDTO toCluster(InternalClusterState clusterState);

  @Mapping(target = "zooKeeperStatus", ignore = true)
  ClusterStatsDTO toClusterStats(InternalClusterState clusterState);

  default ClusterMetricsDTO toClusterMetrics(Metrics metrics) {
    return new ClusterMetricsDTO()
        .items(metrics.getSummarizedMetrics().map(this::convert).toList());
  }

  private MetricDTO convert(RawMetric rawMetric) {
    return new MetricDTO()
        .name(rawMetric.name())
        .labels(rawMetric.labels())
        .value(rawMetric.value());
  }

  default BrokerMetricsDTO toBrokerMetrics(List<RawMetric> metrics) {
    return new BrokerMetricsDTO()
        .metrics(metrics.stream().map(this::convert).toList());
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

  PartitionDTO toPartition(InternalPartition topic);

  BrokerDTO toBrokerDto(InternalBroker broker);

  @Mapping(target = "keySerde", ignore = true)
  @Mapping(target = "valueSerde", ignore = true)
  TopicDetailsDTO toTopicDetails(InternalTopic topic);

  @Mapping(target = "isReadOnly", source = "readOnly")
  @Mapping(target = "isSensitive", source = "sensitive")
  TopicConfigDTO toTopicConfig(InternalTopicConfig topic);

  ReplicaDTO toReplica(InternalReplica replica);

  default ConnectDTO toKafkaConnect(ClustersProperties.ConnectCluster connect, List<InternalConnectorInfo> connectors) {
    int connectorCount = connectors.size();
    int failedConnectors = 0;
    int tasksCount = 0;
    int failedTasksCount = 0;

    for (InternalConnectorInfo connector : connectors) {
      Optional<ConnectorDTO> internalConnector = Optional.ofNullable(connector.getConnector());

      failedConnectors += internalConnector
          .map(ConnectorDTO::getStatus)
          .map(ConnectorStatusDTO::getState)
          .filter(ConnectorStateDTO.FAILED::equals)
          .map(s -> 1).orElse(0);

      tasksCount += internalConnector.map(c -> c.getTasks().size()).orElse(0);

      for (TaskDTO task : connector.getTasks()) {
        if (task.getStatus() != null && ConnectorTaskStatusDTO.FAILED.equals(task.getStatus().getState())) {
          failedTasksCount += tasksCount;
        }
      }
    }

    return new ConnectDTO()
        .address(connect.getAddress())
        .name(connect.getName())
        .connectorsCount(connectorCount)
        .failedConnectorsCount(failedConnectors)
        .tasksCount(tasksCount)
        .failedTasksCount(failedTasksCount);
  }

  List<ClusterDTO.FeaturesEnum> toFeaturesEnum(List<ClusterFeature> features);

  default List<PartitionDTO> map(Map<Integer, InternalPartition> map) {
    return map.values().stream().map(this::toPartition).toList();
  }

  default BrokerDiskUsageDTO map(Integer id, InternalBrokerDiskUsage internalBrokerDiskUsage) {
    final BrokerDiskUsageDTO brokerDiskUsage = new BrokerDiskUsageDTO();
    brokerDiskUsage.setBrokerId(id);
    brokerDiskUsage.segmentCount((int) internalBrokerDiskUsage.getSegmentCount());
    brokerDiskUsage.segmentSize(internalBrokerDiskUsage.getSegmentSize());
    return brokerDiskUsage;
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
