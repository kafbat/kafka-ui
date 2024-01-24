package io.kafbat.ui.controller;

import io.kafbat.ui.exception.ClusterNotFoundException;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

public abstract class AbstractController {

  protected ClustersStorage clustersStorage;
  protected AccessControlService accessControlService;
  protected AuditService auditService;

  protected KafkaCluster getCluster(String name) {
    return clustersStorage.getClusterByName(name)
        .orElseThrow(() -> new ClusterNotFoundException(
            String.format("Cluster with name '%s' not found", name)));
  }

  protected Mono<Void> validateAccess(AccessContext context) {
    return accessControlService.validateAccess(context);
  }

  protected void audit(AccessContext acxt, Signal<?> sig) {
    auditService.audit(acxt, sig);
  }

  @Autowired
  public void setClustersStorage(ClustersStorage clustersStorage) {
    this.clustersStorage = clustersStorage;
  }

  @Autowired
  public void setAccessControlService(AccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  @Autowired
  public void setAuditService(AuditService auditService) {
    this.auditService = auditService;
  }
}
