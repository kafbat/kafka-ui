package io.kafbat.ui.controller;

import io.kafbat.ui.exception.ClusterNotFoundException;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.CsvWriterService;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

public abstract class AbstractController {

  protected ClustersStorage clustersStorage;
  protected AccessControlService accessControlService;
  protected AuditService auditService;
  protected CsvWriterService csvWriterService;

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
  public void setAuditService(AuditService auditService) {
    this.auditService = auditService;
  }

  @Autowired
  public void setAccessControlService(AccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  @Autowired
  public void setCsvWriterService(CsvWriterService csvWriterService) {
    this.csvWriterService = csvWriterService;
  }

  public <T extends Flux<R>, R> Mono<ResponseEntity<String>> responseToCsv(ResponseEntity<T> response) {
    return responseToCsv(response, (t) -> t);
  }

  public <T, R> Mono<ResponseEntity<String>> responseToCsv(ResponseEntity<T> response, Function<T, Flux<R>> extract) {
    if (response.getStatusCode().is2xxSuccessful()) {
      return mapToCsv(extract.apply(response.getBody())).map(ResponseEntity::ok);
    } else {
      return Mono.just(ResponseEntity.status(response.getStatusCode()).body(
          Optional.ofNullable(response.getBody()).map(Object::toString).orElse("")
      ));
    }
  }

  protected <T> Mono<String> mapToCsv(Flux<T> body) {
    return csvWriterService.write(body);
  }
}
