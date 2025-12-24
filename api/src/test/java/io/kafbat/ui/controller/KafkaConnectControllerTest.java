package io.kafbat.ui.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.ConnectorDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Unit tests for KafkaConnectController to verify AccessContext is built correctly.
 * These tests ensure connectorName is passed in operationParams for connector-level
 * permission checking.
 */
@ExtendWith(MockitoExtension.class)
class KafkaConnectControllerTest {

  private static final String CLUSTER_NAME = "test-cluster";
  private static final String CONNECT_NAME = "test-connect";
  private static final String CONNECTOR_NAME = "test-connector";

  @Mock
  private KafkaConnectService kafkaConnectService;

  @Mock
  private AccessControlService accessControlService;

  @Mock
  private ClustersStorage clustersStorage;

  @Mock
  private AuditService auditService;

  @Mock
  private KafkaCluster kafkaCluster;

  @Captor
  private ArgumentCaptor<AccessContext> accessContextCaptor;

  private KafkaConnectController controller;

  @BeforeEach
  void setUp() {
    controller = new KafkaConnectController(kafkaConnectService);
    controller.setAccessControlService(accessControlService);
    controller.setClustersStorage(clustersStorage);
    controller.setAuditService(auditService);

    when(clustersStorage.getClusterByName(CLUSTER_NAME)).thenReturn(Optional.of(kafkaCluster));
    when(accessControlService.validateAccess(any())).thenReturn(Mono.empty());
  }

  @Test
  void getConnector_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.getConnector(any(), any(), any()))
        .thenReturn(Mono.just(new ConnectorDTO()));

    controller.getConnector(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void getConnectorConfig_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.getConnectorConfig(any(), any(), any()))
        .thenReturn(Mono.just(Map.of()));

    controller.getConnectorConfig(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void deleteConnector_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.deleteConnector(any(), any(), any()))
        .thenReturn(Mono.empty());

    controller.deleteConnector(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void setConnectorConfig_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.setConnectorConfig(any(), any(), any(), any()))
        .thenReturn(Mono.just(new ConnectorDTO()));

    controller.setConnectorConfig(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, Mono.just(Map.of()), null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void updateConnectorState_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.updateConnectorState(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    controller.updateConnectorState(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void getConnectorTasks_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.getConnectorTasks(any(), any(), any()))
        .thenReturn(reactor.core.publisher.Flux.empty());

    controller.getConnectorTasks(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void restartConnectorTask_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.restartConnectorTask(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    controller.restartConnectorTask(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, 0, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }

  @Test
  void resetConnectorOffsets_shouldPassConnectorNameInOperationParams() {
    when(kafkaConnectService.resetConnectorOffsets(any(), any(), any()))
        .thenReturn(Mono.empty());

    controller.resetConnectorOffsets(CLUSTER_NAME, CONNECT_NAME, CONNECTOR_NAME, null).block();

    verify(accessControlService).validateAccess(accessContextCaptor.capture());
    AccessContext context = accessContextCaptor.getValue();

    assertThat(context.operationParams()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) context.operationParams();
    assertThat(params).containsEntry("connectorName", CONNECTOR_NAME);
  }
}
