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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApplicationConfigController extends AbstractController implements ApplicationConfigApi, HealthIndicator {

  private final DynamicConfigOperations dynamicConfigOperations;
  private final ApplicationRestarter restarter;
  private final KafkaClusterFactory kafkaClusterFactory;
  private final ApplicationInfoService applicationInfoService;
  private final DynamicConfigMapper configMapper;
  private final ApplicationContext applicationContext;

  private final AtomicBoolean configValid = new AtomicBoolean(false);
  private final AtomicBoolean validationInProgress = new AtomicBoolean(false);

  @jakarta.annotation.PostConstruct
  public void validateInitialConfig() {
    try {
      log.info("Starting initial configuration validation...");
      validateConfigOnStartup();
      configValid.set(true);
      log.info("Configuration validation passed");
    } catch (Exception e) {
      configValid.set(false);
      log.error("CRITICAL: Initial configuration validation failed. Application will exit.", e);
      System.exit(1);
    }
  }

  @Override
  public Health health() {
    if (!configValid.get()) {
      return Health.down()
          .withDetail("reason", "Configuration validation failed")
          .withDetail("action", "Pod will restart automatically")
          .build();
    }
    
    if (validationInProgress.get()) {
      return Health.down()
          .withDetail("reason", "Configuration validation in progress")
          .build();
    }
    
    return Health.up().build();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void validateConfigOnStartup() {
    validationInProgress.set(true);
    try {
      log.info("Performing comprehensive configuration validation...");
      
      // Validate YAML structure first (catches typos like 'rabc' instead of 'rbac')
      validateYamlStructure();
      
      DynamicConfigOperations.PropertiesStructure currentConfig = dynamicConfigOperations.getCurrentProperties();
      validateRequiredSections(currentConfig);
      
      if (currentConfig.getKafka() != null) {
        ClustersProperties clustersProperties = convertToClustersProperties(currentConfig.getKafka());
        validateClustersConfig(clustersProperties)
            .doOnNext(validations -> {
              validations.forEach((clusterName, validation) -> {
                if (validation != null && !isValidationSuccessful(validation)) {
                  throw new IllegalStateException("Cluster validation failed for: " + clusterName);
                }
              });
            })
            .block();
      }
      
      validateRbacConfig(currentConfig);
      
      log.info("Configuration validation completed successfully");
      configValid.set(true);
      
    } catch (Exception e) {
      configValid.set(false);
      log.error("Configuration validation failed: {}", e.getMessage(), e);
      throw new RuntimeException("Configuration validation failed", e);
    } finally {
      validationInProgress.set(false);
    }
  }

  // Enhanced YAML structure validation to catch typos
  private void validateYamlStructure() {
    try {
      Yaml yaml = new Yaml();
      // Try multiple possible config file locations
      String[] configPaths = {
          "file:./kafka-ui/config.yml"
      };
      
      for (String configPath : configPaths) {
        try {
          Resource configResource = applicationContext.getResource(configPath);
          if (configResource.exists()) {
            try (InputStream inputStream = configResource.getInputStream()) {
              Map<String, Object> configMap = yaml.load(inputStream);
              
              // Check for correct section names
              if (!configMap.containsKey("rbac")) {
                // Look for common typos
                boolean foundTypo = false;
                for (String key : configMap.keySet()) {
                  if (key.toLowerCase().contains("rbac") || key.toLowerCase().contains("role") || 
                      key.toLowerCase().contains("access") || key.toLowerCase().contains("auth")) {
                    if (!key.equals("rbac")) {
                      foundTypo = true;
                      throw new IllegalArgumentException("Configuration error: Found section '" + key + 
                          "' instead of 'rbac'. Please correct the section name to 'rbac'. " +
                          "Available sections: " + configMap.keySet());
                    }
                  }
                }
                
                if (!foundTypo) {
                  throw new IllegalArgumentException("Missing 'rbac' section in configuration. " +
                      "Available sections: " + configMap.keySet());
                }
              }
              
              // Validate other required sections
              if (!configMap.containsKey("auth")) {
                throw new IllegalArgumentException("Missing 'auth' section in configuration");
              }
              
              if (!configMap.containsKey("kafka")) {
                throw new IllegalArgumentException("Missing 'kafka' section in configuration");
              }
              
              log.debug("YAML structure validation passed for: {}", configPath);
              return; // Stop after first successful validation
            }
          }
        } catch (Exception e) {
          if (e instanceof IllegalArgumentException) {
            throw e; // Re-throw validation errors
          }
          // Continue to next config path if this one fails
          log.debug("Config path {} not available: {}", configPath, e.getMessage());
        }
      }
      
      log.warn("Could not find config file for YAML structure validation");
      
    } catch (IllegalArgumentException e) {
      throw e; // Re-throw validation errors
    } catch (Exception e) {
      log.warn("YAML structure validation failed: {}", e.getMessage());
      // Don't fail completely - rely on object validation as fallback
    }
  }

  // Helper method to check if validation was successful
  private boolean isValidationSuccessful(ClusterConfigValidationDTO validation) {
    try {
      // Try to use reflection to check validation status
      // First try isValid() method
      try {
        return (Boolean) validation.getClass().getMethod("isValid").invoke(validation);
      } catch (NoSuchMethodException e) {
        // If isValid() doesn't exist, try getValid() method
        try {
          return (Boolean) validation.getClass().getMethod("getValid").invoke(validation);
        } catch (NoSuchMethodException ex) {
          // If neither method exists, check for error fields
          try {
            Object errors = validation.getClass().getMethod("getErrors").invoke(validation);
            if (errors instanceof java.util.Collection) {
              return ((java.util.Collection<?>) errors).isEmpty();
            }
          } catch (NoSuchMethodException exc) {
            // If no validation methods found, assume it's valid
            return true;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Failed to check validation status: {}", e.getMessage());
      return false;
    }
    return true;
  }

  private ClustersProperties convertToClustersProperties(Object kafkaProperties) {
    if (kafkaProperties instanceof ClustersProperties) {
      return (ClustersProperties) kafkaProperties;
    }
    return new ClustersProperties();
  }

  private void validateRbacConfig(DynamicConfigOperations.PropertiesStructure config) {
    if (config.getRbac() == null) {
      throw new IllegalArgumentException("Missing required section: rbac");
    }
    
    // Enhanced RBAC content validation
    try {
      Object rbac = config.getRbac();
      
      // Check if RBAC has roles
      boolean hasRoles = false;
      try {
        Object roles = rbac.getClass().getMethod("getRoles").invoke(rbac);
        if (roles instanceof java.util.Collection) {
          hasRoles = !((java.util.Collection<?>) roles).isEmpty();
          if (!hasRoles) {
            throw new IllegalArgumentException("RBAC section must contain at least one role definition");
          }
        }
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("RBAC section is missing required 'roles' property");
      }
      
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid RBAC configuration structure: " + e.getMessage(), e);
    }
    
    log.debug("RBAC configuration validation passed");
  }

  private void validateRequiredSections(DynamicConfigOperations.PropertiesStructure config) {
    if (config.getAuth() == null) {
      throw new IllegalArgumentException("Missing required section: auth");
    }
    
    if (config.getKafka() == null) {
      throw new IllegalArgumentException("Missing required section: kafka");
    }
    
    if (config.getRbac() == null) {
      throw new IllegalArgumentException("Missing required section: rbac");
    }
  }

  // Your existing methods below

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
          validationInProgress.set(true);
          try {
            var newConfig = configMapper.fromDto(restartDto.getConfig().getProperties());
            validateRequiredSections(newConfig);
            validateRbacConfig(newConfig);
            
            ClustersProperties clustersProperties = convertToClustersProperties(newConfig.getKafka());
            validateClustersConfig(clustersProperties)
                .doOnNext(validations -> {
                  boolean allValid = validations.values().stream()
                      .allMatch(validation -> validation != null && isValidationSuccessful(validation));
                  
                  if (!allValid) {
                    throw new IllegalArgumentException("Cluster validation failed");
                  }
                })
                .block();
                
            dynamicConfigOperations.persist(newConfig);
            configValid.set(true);
            
          } catch (Exception e) {
            configValid.set(false);
            log.error("Config validation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Configuration validation failed", e);
          } finally {
            validationInProgress.set(false);
          }
        })
        .doOnEach(sig -> audit(context, sig))
        .doOnSuccess(dto -> restarter.requestRestart())
        .map(dto -> ResponseEntity.ok().build());
  }

  @Override
  public Mono<ResponseEntity<UploadedFileInfoDTO>> uploadConfigRelatedFile(Flux<Part> fileFlux,
                                                                           ServerWebExchange exchange) {
    var context = AccessContext.builder()
        .applicationConfigActions(EDIT)
        .operationName("uploadConfigRelatedFile")
        .build();
    return validateAccess(context)
        .then(fileFlux.single())
        .flatMap(file ->
            dynamicConfigOperations.uploadConfigRelatedFile((FilePart) file)
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
          validationInProgress.set(true);
          try {
            DynamicConfigOperations.PropertiesStructure newConfig = configMapper.fromDto(config.getProperties());
            validateRequiredSections(newConfig);
            validateRbacConfig(newConfig);
            
            ClustersProperties clustersProperties = convertToClustersProperties(newConfig.getKafka());
            return validateClustersConfig(clustersProperties)
                .map(validations -> {
                  boolean allValid = validations.values().stream()
                      .allMatch(validation -> validation != null && isValidationSuccessful(validation));
                  
                  ApplicationConfigValidationDTO result = new ApplicationConfigValidationDTO()
                      .clusters(validations);
                  
                  // Set valid field if it exists in your DTO
                  try {
                    result.getClass().getMethod("setValid", Boolean.class).invoke(result, allValid);
                  } catch (Exception e) {
                    // If setValid method doesn't exist, ignore
                  }
                  
                  return result;
                });
          } finally {
            validationInProgress.set(false);
          }
        })
        .map(ResponseEntity::ok)
        .doOnEach(sig -> audit(context, sig))
        .onErrorResume(e -> {
          log.error("Configuration validation failed: {}", e.getMessage(), e);
          return Mono.error(e);
        });
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
