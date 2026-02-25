package io.kafbat.ui.controller;

import static io.kafbat.ui.model.rbac.permission.ApplicationConfigAction.EDIT;
import static io.kafbat.ui.model.rbac.permission.ApplicationConfigAction.VIEW;

import io.kafbat.ui.api.ApplicationConfigApi;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.mapper.DynamicConfigMapper;
import io.kafbat.ui.model.AppAuthenticationSettingsDTO;
import io.kafbat.ui.model.ApplicationConfigDTO;
import io.kafbat.ui.model.ApplicationConfigValidationDTO;
import io.kafbat.ui.model.ApplicationInfoDTO;
import io.kafbat.ui.model.ClusterConfigValidationDTO;
import io.kafbat.ui.model.RestartRequestDTO;
import io.kafbat.ui.model.UploadedFileInfoDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ApplicationInfoService;
import io.kafbat.ui.service.KafkaClusterFactory;
import io.kafbat.ui.util.ApplicationRestarter;
import io.kafbat.ui.util.DynamicConfigOperations;
import java.util.Map;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApplicationConfigController extends AbstractController implements ApplicationConfigApi {

  private final DynamicConfigOperations dynamicConfigOperations;
  private final ApplicationRestarter restarter;
  private final KafkaClusterFactory kafkaClusterFactory;
  private final ApplicationInfoService applicationInfoService;
  private final DynamicConfigMapper configMapper;

  @Override
  public Mono<ResponseEntity<ApplicationInfoDTO>> getApplicationInfo(ServerWebExchange exchange) {
    return Mono.just(applicationInfoService.getApplicationInfo()).map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<AppAuthenticationSettingsDTO>> getAuthenticationSettings(
      ServerWebExchange exchange) {
    return Mono.just(applicationInfoService.getAuthenticationProperties())
        .map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<ApplicationConfigDTO>> getCurrentConfig(ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .applicationConfigActions(VIEW)
        .operationName("getCurrentConfig")
        .build();
    return validateAccess(context)
        .then(Mono.fromSupplier(() -> ResponseEntity.ok(
            new ApplicationConfigDTO()
                .properties(configMapper.toDto(dynamicConfigOperations.getCurrentProperties()))
        )))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<Void>> restartWithConfig(Mono<RestartRequestDTO> restartRequestDto,
                                                      ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .applicationConfigActions(EDIT)
        .operationName("restartWithConfig")
        .build();
    return validateAccess(context)
        .then(restartRequestDto)
        .doOnNext(restartDto -> {
          var newConfig = configMapper.fromDto(restartDto.getConfig().getProperties());
          dynamicConfigOperations.persist(newConfig);
        })
        .doOnEach(sig -> audit(context, sig))
        .doOnSuccess(dto -> restarter.requestRestart())
        .map(dto -> ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<UploadedFileInfoDTO>> uploadConfigRelatedFile(Part file,
                                                                           ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .applicationConfigActions(EDIT)
        .operationName("uploadConfigRelatedFile")
        .build();
    return validateAccess(context)
        .then(dynamicConfigOperations.uploadConfigRelatedFile((FilePart) file)
            .map(path -> new UploadedFileInfoDTO(path.toString()))
            .map(ResponseEntity::ok))
        .doOnEach(sig -> audit(context, sig));
  }

  @Override
  public Mono<ResponseEntity<ApplicationConfigValidationDTO>> validateConfig(Mono<ApplicationConfigDTO> configDto,
                                                                             ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .applicationConfigActions(EDIT)
        .operationName("validateConfig")
        .build();
    return validateAccess(context)
        .then(configDto)
        .flatMap(config -> {
          DynamicConfigOperations.PropertiesStructure newConfig = configMapper.fromDto(config.getProperties());
          ClustersProperties clustersProperties = newConfig.getKafka();
          return validateClustersConfig(clustersProperties)
              .map(validations -> new ApplicationConfigValidationDTO().clusters(validations));
        })
        .map(ResponseEntity::ok)
        .doOnEach(sig -> audit(context, sig));
  }

  private Mono<Map<String, ClusterConfigValidationDTO>> validateClustersConfig(
      @Nullable ClustersProperties properties) {
    if (properties == null || properties.getClusters() == null) {
      return Mono.just(Map.of());
    }
    properties.validateAndSetDefaults();
    return Flux.fromIterable(properties.getClusters())
        .flatMap(c -> kafkaClusterFactory.validate(c).map(v -> Tuples.of(c.getName(), v)))
        .collectMap(Tuple2::getT1, Tuple2::getT2);
  }
}
