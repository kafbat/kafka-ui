package io.kafbat.ui.controller;

import io.kafbat.ui.api.AclsApi;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.CreateConsumerAclDTO;
import io.kafbat.ui.model.CreateProducerAclDTO;
import io.kafbat.ui.model.CreateStreamAppAclDTO;
import io.kafbat.ui.model.KafkaAclDTO;
import io.kafbat.ui.model.KafkaAclNamePatternTypeDTO;
import io.kafbat.ui.model.KafkaAclResourceTypeDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.permission.AclAction;
import io.kafbat.ui.service.acl.AclsService;
import io.kafbat.ui.service.mcp.McpTool;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public  class AclsController extends AbstractController implements AclsApi, McpTool {

  private final AclsService aclsService;

  @Override
  public Mono<ResponseEntity<Void>> createAcl(String clusterName, Mono<KafkaAclDTO> kafkaAclDto,
                                              ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("createAcl")
        .build();

    return validateAccess(context)
        .then(kafkaAclDto)
        .map(ClusterMapper::toAclBinding)
        .flatMap(binding -> aclsService.createAcl(getCluster(clusterName), binding))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteAcl(String clusterName, Mono<KafkaAclDTO> kafkaAclDto,
                                              ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("deleteAcl")
        .build();

    return validateAccess(context)
        .then(kafkaAclDto)
        .map(ClusterMapper::toAclBinding)
        .flatMap(binding -> aclsService.deleteAcl(getCluster(clusterName), binding))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Flux<KafkaAclDTO>>> listAcls(String clusterName,
                                                          KafkaAclResourceTypeDTO resourceTypeDto,
                                                          String resourceName,
                                                          KafkaAclNamePatternTypeDTO namePatternTypeDto,
                                                          String search,
                                                          Boolean fts,
                                                          ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.VIEW)
        .operationName("listAcls")
        .build();

    var resourceType = Optional.ofNullable(resourceTypeDto)
        .map(ClusterMapper::mapAclResourceTypeDto)
        .orElse(ResourceType.ANY);

    var namePatternType = Optional.ofNullable(namePatternTypeDto)
        .map(ClusterMapper::mapPatternTypeDto)
        .orElse(PatternType.ANY);

    var filter = new ResourcePatternFilter(resourceType, resourceName, namePatternType);

    return validateAccess(context).then(
        Mono.just(
            ResponseEntity.ok(
                aclsService.listAcls(getCluster(clusterName), filter, search, fts)
                    .map(ClusterMapper::toKafkaAclDto)))
    ).doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<String>> getAclAsCsv(String clusterName, ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.VIEW)
        .operationName("getAclAsCsv")
        .build();

    return validateAccess(context).then(
        aclsService.getAclAsCsvString(getCluster(clusterName))
            .map(ResponseEntity::ok)
            .flatMap(Mono::just)
            .doOnEach(sig -> audit(context, sig))
    );
  }

  @Override
  public Mono<ResponseEntity<Void>> syncAclsCsv(String clusterName, Mono<String> csvMono, ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("syncAclsCsv")
        .build();

    return validateAccess(context)
        .then(csvMono)
        .flatMap(csv -> aclsService.syncAclWithAclCsv(getCluster(clusterName), csv))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> createConsumerAcl(String clusterName,
                                                      Mono<CreateConsumerAclDTO> createConsumerAclDto,
                                                      ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("createConsumerAcl")
        .build();

    return validateAccess(context)
        .then(createConsumerAclDto)
        .flatMap(req -> aclsService.createConsumerAcl(getCluster(clusterName), req))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> createProducerAcl(String clusterName,
                                                      Mono<CreateProducerAclDTO> createProducerAclDto,
                                                      ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("createProducerAcl")
        .build();

    return validateAccess(context)
        .then(createProducerAclDto)
        .flatMap(req -> aclsService.createProducerAcl(getCluster(clusterName), req))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<Void>> createStreamAppAcl(String clusterName,
                                                       Mono<CreateStreamAppAclDTO> createStreamAppAclDto,
                                                       ServerWebExchange exchange) {
    AccessContext context = AccessContext.builder()
        .cluster(clusterName)
        .aclActions(AclAction.EDIT)
        .operationName("createStreamAppAcl")
        .build();

    return validateAccess(context)
        .then(createStreamAppAclDto)
        .flatMap(req -> aclsService.createStreamAppAcl(getCluster(clusterName), req))
        .doOnEach(sig -> audit(context, sig))
        .thenReturn(ResponseEntity.ok().build());
  }
}
