package io.kafbat.ui.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.kafbat.ui.exception.SchemaCompatibilityException;
import io.kafbat.ui.exception.SchemaNotFoundException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.kafbat.ui.sr.api.KafkaSrClientApi;
import io.kafbat.ui.sr.model.Compatibility;
import io.kafbat.ui.sr.model.CompatibilityCheckResponse;
import io.kafbat.ui.sr.model.CompatibilityConfig;
import io.kafbat.ui.sr.model.CompatibilityLevelChange;
import io.kafbat.ui.sr.model.NewSubject;
import io.kafbat.ui.sr.model.SchemaSubject;
import io.kafbat.ui.util.ReactiveFailover;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchemaRegistryService {
  private static final String LATEST = "latest";

  @AllArgsConstructor
  public static class SubjectWithCompatibilityLevel {
    @Delegate
    SchemaSubject subject;
    @Getter
    Compatibility compatibility;
    @Getter
    String topic;
  }

  private final StatisticsCache statisticsCache;

  private ReactiveFailover<KafkaSrClientApi> api(KafkaCluster cluster) {
    return cluster.getSchemaRegistryClient();
  }

  public Mono<List<SubjectWithCompatibilityLevel>> getAllLatestVersionSchemas(KafkaCluster cluster,
                                                                              List<String> subjects,
                                                                              int pageSize) {
    return Flux.fromIterable(subjects)
        .flatMap(subject -> getLatestSchemaVersionBySubject(cluster, subject), pageSize)
        .collect(Collectors.toList());
  }

  public Mono<List<String>> getAllSubjectNames(KafkaCluster cluster) {
    return api(cluster)
        .mono(c -> c.getAllSubjectNames(null, false))
        .flatMapIterable(this::parseSubjectListString)
        .collectList();
  }

  @SneakyThrows
  private List<String> parseSubjectListString(String subjectNamesStr) {
    //workaround for https://github.com/spring-projects/spring-framework/issues/24734
    return new JsonMapper().readValue(subjectNamesStr, new TypeReference<>() {
    });
  }

  public Flux<SubjectWithCompatibilityLevel> getAllVersionsBySubject(KafkaCluster cluster, String subject) {
    Flux<Integer> versions = getSubjectVersions(cluster, subject);
    return versions.flatMap(version -> getSchemaSubjectByVersion(cluster, subject, version));
  }

  private Flux<Integer> getSubjectVersions(KafkaCluster cluster, String schemaName) {
    return api(cluster).flux(c -> c.getSubjectVersions(schemaName));
  }

  public Mono<SubjectWithCompatibilityLevel> getSchemaSubjectByVersion(KafkaCluster cluster,
                                                                       String schemaName,
                                                                       Integer version) {
    return getSchemaSubject(cluster, schemaName, String.valueOf(version));
  }

  public Mono<SubjectWithCompatibilityLevel> getLatestSchemaVersionBySubject(KafkaCluster cluster,
                                                                             String schemaName) {
    return getSchemaSubject(cluster, schemaName, LATEST);
  }

  private String topicName(KafkaCluster cluster, String schemaName) {
    return Optional.ofNullable(
        statisticsCache.get(cluster)
            .getClusterState()
            .getTopicStates()
            .get(schemaName.replace(cluster.getSchemaRegistryTopicSubjectSuffix(), "")))
        .map(ScrapedClusterState.TopicState::name)
        .orElse(null);
  }

  private Mono<SubjectWithCompatibilityLevel> getSchemaSubject(KafkaCluster cluster, String schemaName,
                                                               String version) {
    return api(cluster)
        .mono(c -> c.getSubjectVersion(schemaName, version, false))
        .zipWith(getSchemaCompatibilityInfoOrGlobal(cluster, schemaName))
        .map(t -> new SubjectWithCompatibilityLevel(t.getT1(), t.getT2(), topicName(cluster, schemaName)))
        .onErrorResume(WebClientResponseException.NotFound.class, th -> Mono.error(new SchemaNotFoundException()));
  }

  public Mono<Void> deleteSchemaSubjectByVersion(KafkaCluster cluster, String schemaName, Integer version) {
    return deleteSchemaSubject(cluster, schemaName, String.valueOf(version));
  }

  public Mono<Void> deleteLatestSchemaSubject(KafkaCluster cluster, String schemaName) {
    return deleteSchemaSubject(cluster, schemaName, LATEST);
  }

  private Mono<Void> deleteSchemaSubject(KafkaCluster cluster, String schemaName, String version) {
    return api(cluster).mono(c -> c.deleteSubjectVersion(schemaName, version, false));
  }

  public Mono<Void> deleteSchemaSubjectEntirely(KafkaCluster cluster, String schemaName) {
    return api(cluster).mono(c -> c.deleteAllSubjectVersions(schemaName, false));
  }

  /**
   * Checks whether the provided schema duplicates the previous or not, creates a new schema
   * and then returns the whole content by requesting its latest version.
   */
  public Mono<SubjectWithCompatibilityLevel> registerNewSchema(KafkaCluster cluster,
                                                               String subject,
                                                               NewSubject newSchemaSubject) {
    return api(cluster)
        .mono(c -> c.registerNewSchema(subject, newSchemaSubject))
        .onErrorMap(WebClientResponseException.Conflict.class,
            th -> new SchemaCompatibilityException())
        .onErrorMap(WebClientResponseException.UnprocessableEntity.class,
            th -> new ValidationException("Invalid schema. Error from registry: " + th.getResponseBodyAsString()))
        .then(getLatestSchemaVersionBySubject(cluster, subject));
  }

  public Mono<Void> updateSchemaCompatibility(KafkaCluster cluster,
                                              String schemaName,
                                              Compatibility compatibility) {
    return api(cluster)
        .mono(c -> c.updateSubjectCompatibilityLevel(
            schemaName, new CompatibilityLevelChange().compatibility(compatibility)))
        .then();
  }

  public Mono<Void> updateGlobalSchemaCompatibility(KafkaCluster cluster,
                                                    Compatibility compatibility) {
    return api(cluster)
        .mono(c -> c.updateGlobalCompatibilityLevel(new CompatibilityLevelChange().compatibility(compatibility)))
        .then();
  }

  public Mono<Compatibility> getSchemaCompatibilityLevel(KafkaCluster cluster,
                                                         String schemaName) {
    return api(cluster)
        .mono(c -> c.getSubjectCompatibilityLevel(schemaName, true))
        .map(CompatibilityConfig::getCompatibilityLevel)
        .onErrorResume(error -> Mono.empty());
  }

  public Mono<Compatibility> getGlobalSchemaCompatibilityLevel(KafkaCluster cluster) {
    return api(cluster)
        .mono(KafkaSrClientApi::getGlobalCompatibilityLevel)
        .map(CompatibilityConfig::getCompatibilityLevel);
  }

  private Mono<Compatibility> getSchemaCompatibilityInfoOrGlobal(KafkaCluster cluster,
                                                                 String schemaName) {
    return getSchemaCompatibilityLevel(cluster, schemaName)
        .switchIfEmpty(this.getGlobalSchemaCompatibilityLevel(cluster));
  }

  public Mono<CompatibilityCheckResponse> checksSchemaCompatibility(KafkaCluster cluster,
                                                                    String schemaName,
                                                                    NewSubject newSchemaSubject) {
    return api(cluster).mono(c -> c.checkSchemaCompatibility(schemaName, LATEST, true, newSchemaSubject));
  }
}
