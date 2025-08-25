package io.kafbat.ui.config.validation;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.ClusterConfigValidationDTO;
import io.kafbat.ui.service.KafkaClusterFactory;
import io.kafbat.ui.util.DynamicConfigOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigValidationHealthIndicator implements ReactiveHealthIndicator {

    private final DynamicConfigOperations dynamicConfigOperations;
    private final KafkaClusterFactory kafkaClusterFactory;

    private final AtomicBoolean configValid = new AtomicBoolean(false);
    private final AtomicReference<String> validationError = new AtomicReference<>("Validation not yet performed");

    @Override
    public Mono<Health> health() {
        if (!configValid.get()) {
            return Mono.just(Health.down()
                .withDetail("reason", "Configuration validation failed")
                .withDetail("error", validationError.get())
                .build());
        }

        return Mono.just(Health.up().build());
    }

    public Mono<Boolean> validateConfiguration() {
        try {
            // getCurrentProperties() returns the object directly, not a Mono
            DynamicConfigOperations.PropertiesStructure currentConfig = dynamicConfigOperations.getCurrentProperties();
            boolean isValid = validateConfigStructure(currentConfig);

            configValid.set(isValid);
            if (isValid) {
                validationError.set(null);
                log.info("Configuration validation passed");
            }

            return Mono.just(isValid);

        } catch (Exception e) {
            configValid.set(false);
            validationError.set(e.getMessage());
            log.error("Configuration validation failed: {}", e.getMessage());
            return Mono.just(false);
        }
    }

    private boolean validateConfigStructure(DynamicConfigOperations.PropertiesStructure config) {
        try {
            // Validate required sections
            validateRequiredSections(config);

            // Validate clusters
            boolean clustersValid = validateClusters(config.getKafka());
            if (!clustersValid) {
                throw new IllegalArgumentException("Cluster validation failed");
            }

            return true;

        } catch (Exception e) {
            throw new RuntimeException("Configuration validation failed: " + e.getMessage(), e);
        }
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

        // Validate that sections have basic structure
        validateSectionStructure("auth", config.getAuth());
        validateSectionStructure("kafka", config.getKafka());
        validateSectionStructure("rbac", config.getRbac());
    }

    private void validateSectionStructure(String sectionName, Object sectionConfig) {
        if (sectionConfig == null) {
            throw new IllegalArgumentException(sectionName + " section is null");
        }

        // Basic validation based on section type
        switch (sectionName) {
            case "kafka":
                validateKafkaStructure(sectionConfig);
                break;
            case "rbac":
                validateRbacStructure(sectionConfig);
                break;
            case "auth":
                validateAuthStructure(sectionConfig);
                break;
        }
    }

    private void validateAuthStructure(Object authConfig) {
        // Basic validation - check if auth config has basic structure
        try {
            String authString = authConfig.toString();
            if (authString.contains("null") || authString.isEmpty()) {
                throw new IllegalArgumentException("Auth configuration appears to be empty or invalid");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid auth configuration: " + e.getMessage());
        }
    }

    private void validateKafkaStructure(Object kafkaConfig) {
        if (!(kafkaConfig instanceof ClustersProperties)) {
            throw new IllegalArgumentException("Invalid kafka configuration type");
        }

        ClustersProperties kafka = (ClustersProperties) kafkaConfig;

        // Failure-fast: explicit error if clusters are missing or empty
        if (kafka.getClusters() == null) {
            throw new IllegalArgumentException("Kafka configuration missing 'clusters' property");
        }

        if (kafka.getClusters().isEmpty()) {
            throw new IllegalArgumentException("Kafka clusters list cannot be empty");
        }

        // Validate each cluster has required properties
        kafka.getClusters().forEach(cluster -> {
            if (cluster.getName() == null || cluster.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Kafka cluster missing 'name' property");
            }
            if (cluster.getBootstrapServers() == null || cluster.getBootstrapServers().trim().isEmpty()) {
                throw new IllegalArgumentException("Kafka cluster '" + cluster.getName() + "' missing 'bootstrapServers' property");
            }
        });
    }

    private void validateRbacStructure(Object rbacConfig) {
        // Basic validation using toString checks to avoid reflection
        try {
            String rbacString = rbacConfig.toString();
            if (rbacString.contains("roles=null") || !rbacString.contains("roles")) {
                throw new IllegalArgumentException("RBAC section missing 'roles' property");
            }

            if (rbacString.contains("roles=[]") || rbacString.contains("roles=[]")) {
                throw new IllegalArgumentException("RBAC roles list cannot be empty");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RBAC configuration: " + e.getMessage());
        }
    }

    private boolean validateClusters(Object kafkaProperties) {
        if (!(kafkaProperties instanceof ClustersProperties)) {
            return false;
        }

        ClustersProperties clustersProperties = (ClustersProperties) kafkaProperties;

        if (clustersProperties.getClusters() == null || clustersProperties.getClusters().isEmpty()) {
            return false;
        }

        clustersProperties.validateAndSetDefaults();

        // For synchronous validation, we can't use reactive streams easily
        // This is a simplified synchronous validation
        try {
            clustersProperties.getClusters().forEach(cluster -> {
                // This will throw an exception if validation fails
                kafkaClusterFactory.validate(cluster).block();
            });
            return true;
        } catch (Exception e) {
            log.warn("Cluster validation failed: {}", e.getMessage());
            return false;
        }
    }
}
