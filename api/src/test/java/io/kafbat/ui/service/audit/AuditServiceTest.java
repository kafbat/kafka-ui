package io.kafbat.ui.service.audit;

import static io.kafbat.ui.service.audit.AuditService.createAuditWriter;
import static io.kafbat.ui.service.audit.AuditService.createTopicIfNeeded;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.service.ReactiveAdminClient;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

@SuppressWarnings("unchecked")
class AuditServiceTest {

  @Test
  void isAuditTopicChecksIfAuditIsEnabledForCluster() {
    Map<String, AuditWriter> writers = Map.of(
        "c1", new AuditWriter("с1", true, "c1topic", null, null),
        "c2", new AuditWriter("c2", false, "c2topic", mock(KafkaProducer.class), null)
    );

    var auditService = new AuditService(writers);
    assertThat(auditService.isAuditTopic(KafkaCluster.builder().name("notExist").build(), "some"))
        .isFalse();
    assertThat(auditService.isAuditTopic(KafkaCluster.builder().name("c1").build(), "c1topic"))
        .isFalse();
    assertThat(auditService.isAuditTopic(KafkaCluster.builder().name("c2").build(), "c2topic"))
        .isTrue();
  }

  @Test
  void auditCallsWriterMethodDependingOnSignal() {
    var auditWriter = Mockito.mock(AuditWriter.class);
    var auditService = new AuditService(Map.of("test", auditWriter));

    var cxt = AccessContext.builder().cluster("test").build();

    auditService.audit(cxt, Signal.complete());
    verify(auditWriter).write(any(), any(), eq(null));

    var th = new Exception("testError");
    auditService.audit(cxt, Signal.error(th));
    verify(auditWriter).write(any(), any(), eq(th));
  }

  @SuppressWarnings("unchecked")
  @Nested
  class CreateAuditWriter {

    private final ReactiveAdminClient adminClientMock = mock(ReactiveAdminClient.class);
    private final Supplier<KafkaProducer<byte[], byte[]>> producerSupplierMock = mock(Supplier.class);

    private final ClustersProperties.Cluster clustersProperties = new ClustersProperties.Cluster();

    private final KafkaCluster cluster = KafkaCluster
        .builder()
        .name("test")
        .originalProperties(clustersProperties)
        .build();


    @BeforeEach
    void init() {
      when(producerSupplierMock.get())
          .thenReturn(mock(KafkaProducer.class));
    }

    @Test
    void logOnlyAlterOpsByDefault() {
      var auditProps = new ClustersProperties.AuditProperties();
      auditProps.setConsoleAuditEnabled(true);
      clustersProperties.setAudit(auditProps);

      var maybeWriter = createAuditWriter(cluster, () -> adminClientMock, producerSupplierMock);
      assertThat(maybeWriter)
          .hasValueSatisfying(w -> assertThat(w.logAlterOperationsOnly()).isTrue());
    }

    @Test
    void noWriterIfNoAuditPropsSet() {
      var maybeWriter = createAuditWriter(cluster, () -> adminClientMock, producerSupplierMock);
      assertThat(maybeWriter).isEmpty();
    }

    @Test
    void setsLoggerIfConsoleLoggingEnabled() {
      var auditProps = new ClustersProperties.AuditProperties();
      auditProps.setConsoleAuditEnabled(true);
      clustersProperties.setAudit(auditProps);

      var maybeWriter = createAuditWriter(cluster, () -> adminClientMock, producerSupplierMock);
      assertThat(maybeWriter).isPresent();

      var writer = maybeWriter.get();
      assertThat(writer.consoleLogger()).isNotNull();
    }

    @Nested
    class WhenTopicAuditEnabled {
      private static final String AUDIT_TOPIC = "test_audit_topic";

      @BeforeEach
      void setTopicWriteProperties() {
        var auditProps = new ClustersProperties.AuditProperties();
        auditProps.setTopicAuditEnabled(true);
        auditProps.setTopic(AUDIT_TOPIC);
        auditProps.setAuditTopicsPartitions(3);
        auditProps.setAuditTopicProperties(Map.of("p1", "v1"));
        clustersProperties.setAudit(auditProps);
      }

      @Test
      void createsProducerIfTopicExists() {
        when(adminClientMock.listTopics(true))
            .thenReturn(Mono.just(Set.of(AUDIT_TOPIC)));

        var maybeWriter = createAuditWriter(cluster, () -> adminClientMock, producerSupplierMock);
        assertThat(maybeWriter).isPresent();

        //checking there was no topic creation request
        verify(adminClientMock, times(0))
            .createTopic(any(), anyInt(), anyInt(), anyMap());

        var writer = maybeWriter.get();
        assertThat(writer.producer()).isNotNull();
        assertThat(writer.targetTopic()).isEqualTo(AUDIT_TOPIC);
      }

      @Test
      void createsProducerAndTopicIfItIsNotExist() {
        when(adminClientMock.listTopics(true))
            .thenReturn(Mono.just(Set.of()));

        when(adminClientMock.createTopic(eq(AUDIT_TOPIC), eq(3), eq(null), anyMap()))
            .thenReturn(Mono.empty());

        var maybeWriter = createAuditWriter(cluster, () -> adminClientMock, producerSupplierMock);
        assertThat(maybeWriter).isPresent();

        //verifying topic created
        verify(adminClientMock).createTopic(eq(AUDIT_TOPIC), eq(3), eq(null), anyMap());

        var writer = maybeWriter.get();
        assertThat(writer.producer()).isNotNull();
        assertThat(writer.targetTopic()).isEqualTo(AUDIT_TOPIC);
      }

      @Test
      void throwExceptionIfRequireAuditTopic() {
        var auditProps = clustersProperties.getAudit();
        auditProps.setRequireAuditTopic(true);
        clustersProperties.setAudit(auditProps);

        var exp = assertThrows(RuntimeException.class,
            () -> createTopicIfNeeded(cluster, () -> {
              throw new RuntimeException(); }, AUDIT_TOPIC, auditProps));

        assertEquals("Error while connecting to the cluster to create the audit topic '" + AUDIT_TOPIC + "'",
            exp.getMessage());

        exp = assertThrows(RuntimeException.class,
            () -> createTopicIfNeeded(cluster, () -> null, AUDIT_TOPIC, auditProps));

        assertEquals("Error while checking the existence of the audit topic '" + AUDIT_TOPIC + "'", exp.getMessage());

        when(adminClientMock.listTopics(true)).thenReturn(Mono.just(Set.of()));
        when(adminClientMock.createTopic(eq(AUDIT_TOPIC), eq(3), eq(null), anyMap()))
            .thenReturn(Mono.error(new KafkaException()));

        exp = assertThrows(RuntimeException.class,
            () -> createTopicIfNeeded(cluster, () -> adminClientMock, AUDIT_TOPIC, auditProps));

        assertEquals("Error creating the audit topic '" + AUDIT_TOPIC + "'", exp.getMessage());
      }

    }
  }


}
