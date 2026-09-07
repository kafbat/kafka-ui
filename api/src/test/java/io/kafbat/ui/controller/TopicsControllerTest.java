package io.kafbat.ui.controller;

import static io.kafbat.ui.model.rbac.Resource.TOPIC;
import static io.kafbat.ui.model.rbac.permission.TopicAction.EDIT;
import static io.kafbat.ui.model.rbac.permission.TopicAction.VIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.PartitionsIncreaseDTO;
import io.kafbat.ui.model.PartitionsIncreaseResponseDTO;
import io.kafbat.ui.model.ReplicationFactorChangeDTO;
import io.kafbat.ui.model.ReplicationFactorChangeResponseDTO;
import io.kafbat.ui.model.TopicDTO;
import io.kafbat.ui.model.TopicUpdateDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.TopicsService;
import io.kafbat.ui.service.acl.AclsService;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TopicsControllerTest {

  private static final String CLUSTER_NAME = "local";
  private static final String TOPIC_NAME = "orders";

  private final TopicsService topicsService = mock(TopicsService.class);
  private final ClusterMapper clusterMapper = mock(ClusterMapper.class);
  private final ClustersStorage clustersStorage = mock(ClustersStorage.class);
  private final AccessControlService accessControlService = mock(AccessControlService.class);
  private final AuditService auditService = mock(AuditService.class);
  private final KafkaCluster cluster = KafkaCluster.builder().name(CLUSTER_NAME).build();

  private TopicsController controller;

  @BeforeEach
  void setUp() {
    controller = new TopicsController(
        topicsService,
        mock(TopicAnalysisService.class),
        clusterMapper,
        mock(ClustersProperties.class),
        mock(KafkaConnectService.class),
        mock(AclsService.class)
    );
    controller.setClustersStorage(clustersStorage);
    controller.setAccessControlService(accessControlService);
    controller.setAuditService(auditService);

    when(clustersStorage.getClusterByName(CLUSTER_NAME)).thenReturn(Optional.of(cluster));
    when(accessControlService.validateAccess(any(AccessContext.class))).thenReturn(Mono.empty());
  }

  @Test
  void updateTopicUsesRequestBodyInAccessContext() {
    TopicUpdateDTO topicUpdate = new TopicUpdateDTO()
        .configs(Map.of("cleanup.policy", "compact"));
    InternalTopic internalTopic = InternalTopic.builder()
        .name(TOPIC_NAME)
        .partitions(Map.of())
        .build();
    TopicDTO topic = new TopicDTO().name(TOPIC_NAME);

    when(topicsService.updateTopic(cluster, TOPIC_NAME, topicUpdate)).thenReturn(Mono.just(internalTopic));
    when(clusterMapper.toTopic(internalTopic)).thenReturn(topic);

    StepVerifier.create(controller.updateTopic(CLUSTER_NAME, TOPIC_NAME, Mono.just(topicUpdate), null))
        .assertNext(response -> assertThat(response.getBody()).isSameAs(topic))
        .verifyComplete();

    verify(topicsService).updateTopic(cluster, TOPIC_NAME, topicUpdate);
    assertAccessAndAuditContexts("updateTopic", topicUpdate);
  }

  @Test
  void increaseTopicPartitionsUsesRequestBodyInAccessContext() {
    PartitionsIncreaseDTO partitionsIncrease = new PartitionsIncreaseDTO()
        .totalPartitionsCount(10);
    PartitionsIncreaseResponseDTO response = new PartitionsIncreaseResponseDTO()
        .topicName(TOPIC_NAME)
        .totalPartitionsCount(10);

    when(topicsService.increaseTopicPartitions(cluster, TOPIC_NAME, partitionsIncrease))
        .thenReturn(Mono.just(response));

    StepVerifier.create(
            controller.increaseTopicPartitions(CLUSTER_NAME, TOPIC_NAME, Mono.just(partitionsIncrease), null))
        .assertNext(entity -> assertThat(entity.getBody()).isSameAs(response))
        .verifyComplete();

    verify(topicsService).increaseTopicPartitions(cluster, TOPIC_NAME, partitionsIncrease);
    assertAccessAndAuditContexts("increasePartitions", partitionsIncrease);
  }

  @Test
  void changeReplicationFactorUsesRequestBodyInAccessContext() {
    ReplicationFactorChangeDTO replicationFactorChange = new ReplicationFactorChangeDTO()
        .totalReplicationFactor(3);
    ReplicationFactorChangeResponseDTO response = new ReplicationFactorChangeResponseDTO()
        .topicName(TOPIC_NAME)
        .totalReplicationFactor(3);

    when(topicsService.changeReplicationFactor(cluster, TOPIC_NAME, replicationFactorChange))
        .thenReturn(Mono.just(response));

    StepVerifier.create(
            controller.changeReplicationFactor(CLUSTER_NAME, TOPIC_NAME, Mono.just(replicationFactorChange), null))
        .assertNext(entity -> assertThat(entity.getBody()).isSameAs(response))
        .verifyComplete();

    verify(topicsService).changeReplicationFactor(cluster, TOPIC_NAME, replicationFactorChange);
    assertAccessAndAuditContexts("changeReplicationFactor", replicationFactorChange);
  }

  private void assertAccessAndAuditContexts(String operationName, Object operationParams) {
    ArgumentCaptor<AccessContext> accessContextCaptor = ArgumentCaptor.forClass(AccessContext.class);
    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    assertContext(accessContextCaptor.getValue(), operationName, operationParams);

    ArgumentCaptor<AccessContext> auditContextCaptor = ArgumentCaptor.forClass(AccessContext.class);
    verify(auditService, atLeastOnce()).audit(auditContextCaptor.capture(), any());
    assertThat(auditContextCaptor.getAllValues())
        .allSatisfy(context -> assertContext(context, operationName, operationParams));
  }

  private void assertContext(AccessContext context, String operationName, Object operationParams) {
    assertThat(context.cluster()).isEqualTo(CLUSTER_NAME);
    assertThat(context.operationName()).isEqualTo(operationName);
    assertThat(context.operationParams()).isSameAs(operationParams);
    assertThat(context.accessedResources()).singleElement().satisfies(resource -> {
      assertThat(resource.resourceType()).isEqualTo(TOPIC);
      assertThat(resource.resourceId()).isEqualTo(TOPIC_NAME);
      assertThat(resource.requestedActions().stream().map(PermissibleAction::name).toList())
          .containsExactlyInAnyOrder(VIEW.name(), EDIT.name());
    });
  }
}
