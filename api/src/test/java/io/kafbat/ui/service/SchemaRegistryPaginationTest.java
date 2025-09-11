package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.controller.SchemasController;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.SchemaSubjectDTO;
import io.kafbat.ui.service.audit.AuditService;
import io.kafbat.ui.sr.model.Compatibility;
import io.kafbat.ui.sr.model.SchemaSubject;
import io.kafbat.ui.util.AccessControlServiceMock;
import io.kafbat.ui.util.ReactiveFailover;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class SchemaRegistryPaginationTest {

  private static final String LOCAL_KAFKA_CLUSTER_NAME = "local";

  private SchemasController controller;

  private void init(List<String> subjects) {
    ClustersStorage clustersStorage = Mockito.mock(ClustersStorage.class);
    when(clustersStorage.getClusterByName(isA(String.class)))
        .thenReturn(Optional.of(buildKafkaCluster(LOCAL_KAFKA_CLUSTER_NAME)));

    SchemaRegistryService schemaRegistryService = Mockito.mock(SchemaRegistryService.class);
    when(schemaRegistryService.getAllSubjectNames(isA(KafkaCluster.class)))
                .thenReturn(Mono.just(subjects));
    when(schemaRegistryService
            .getAllLatestVersionSchemas(isA(KafkaCluster.class), anyList())).thenCallRealMethod();
    when(schemaRegistryService.getLatestSchemaVersionBySubject(isA(KafkaCluster.class), isA(String.class)))
            .thenAnswer(a -> Mono.just(
                new SchemaRegistryService.SubjectWithCompatibilityLevel(
                    new SchemaSubject().subject(a.getArgument(1)), Compatibility.FULL)));

    this.controller = new SchemasController(schemaRegistryService, new ClustersProperties());
    this.controller.setAccessControlService(new AccessControlServiceMock().getMock());
    this.controller.setAuditService(mock(AuditService.class));
    this.controller.setClustersStorage(clustersStorage);
  }

  @Test
  void shouldListFirst25andThen10Schemas() {
    init(
            IntStream.rangeClosed(1, 100)
                    .boxed()
                    .map(num -> "subject" + num)
                    .toList()
    );
    var schemasFirst25 = controller.getSchemas(LOCAL_KAFKA_CLUSTER_NAME,
            null, null, null, null).block();
    assertThat(schemasFirst25).isNotNull();
    assertThat(schemasFirst25.getBody()).isNotNull();
    assertThat(schemasFirst25.getBody().getPageCount()).isEqualTo(4);
    assertThat(schemasFirst25.getBody().getSchemas()).hasSize(25);
    assertThat(schemasFirst25.getBody().getSchemas())
            .isSortedAccordingTo(Comparator.comparing(SchemaSubjectDTO::getSubject));

    var schemasFirst10 = controller.getSchemas(LOCAL_KAFKA_CLUSTER_NAME,
            null, 10, null, null).block();

    assertThat(schemasFirst10).isNotNull();
    assertThat(schemasFirst10.getBody()).isNotNull();
    assertThat(schemasFirst10.getBody().getPageCount()).isEqualTo(10);
    assertThat(schemasFirst10.getBody().getSchemas()).hasSize(10);
    assertThat(schemasFirst10.getBody().getSchemas())
            .isSortedAccordingTo(Comparator.comparing(SchemaSubjectDTO::getSubject));
  }

  @Test
  void shouldListSchemasContaining_1() {
    init(
              IntStream.rangeClosed(1, 100)
                      .boxed()
                      .map(num -> "subject" + num)
                      .toList()
    );
    var schemasSearch7 = controller.getSchemas(LOCAL_KAFKA_CLUSTER_NAME,
            null, null, "1", null).block();
    assertThat(schemasSearch7).isNotNull();
    assertThat(schemasSearch7.getBody()).isNotNull();
    assertThat(schemasSearch7.getBody().getPageCount()).isEqualTo(1);
    assertThat(schemasSearch7.getBody().getSchemas()).hasSize(20);
  }

  @Test
  void shouldCorrectlyHandleNonPositivePageNumberAndPageSize() {
    init(
                IntStream.rangeClosed(1, 100)
                        .boxed()
                        .map(num -> "subject" + num)
                        .toList()
    );
    var schemas = controller.getSchemas(LOCAL_KAFKA_CLUSTER_NAME,
            0, -1, null, null).block();

    assertThat(schemas).isNotNull();
    assertThat(schemas.getBody()).isNotNull();
    assertThat(schemas.getBody().getPageCount()).isEqualTo(4);
    assertThat(schemas.getBody().getSchemas()).hasSize(25);
    assertThat(schemas.getBody().getSchemas()).isSortedAccordingTo(Comparator.comparing(SchemaSubjectDTO::getSubject));
  }

  @Test
  void shouldCalculateCorrectPageCountForNonDivisiblePageSize() {
    init(
                IntStream.rangeClosed(1, 100)
                        .boxed()
                        .map(num -> "subject" + num)
                        .toList()
    );

    var schemas = controller.getSchemas(LOCAL_KAFKA_CLUSTER_NAME,
            4, 33, null, null).block();

    assertThat(schemas).isNotNull();
    assertThat(schemas.getBody()).isNotNull();
    assertThat(schemas.getBody().getPageCount()).isEqualTo(4);
    assertThat(schemas.getBody().getSchemas()).hasSize(1);
    assertThat(schemas.getBody().getSchemas().getFirst().getSubject()).isEqualTo("subject99");
  }

  @SuppressWarnings("unchecked")
  private KafkaCluster buildKafkaCluster(String clusterName) {
    return KafkaCluster.builder()
            .name(clusterName)
            .schemaRegistryClient(mock(ReactiveFailover.class))
            .build();
  }
}
