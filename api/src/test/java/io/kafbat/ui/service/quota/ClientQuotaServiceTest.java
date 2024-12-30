package io.kafbat.ui.service.quota;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.ClustersStorage;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class ClientQuotaServiceTest extends AbstractIntegrationTest {

  @Autowired
  ClientQuotaService quotaService;

  private KafkaCluster cluster;

  @BeforeEach
  void init() {
    cluster = applicationContext.getBean(ClustersStorage.class).getClusterByName(LOCAL).orElseThrow();
  }

  @ParameterizedTest
  @CsvSource(
      value = {
          "testUser, null, null ",
          "null, testUserId, null",
          "testUser2, testUserId2, null",
      },
      nullValues = "null"
  )
  void createUpdateDelete(String user, String clientId, String ip) {
    var initialQuotas = Map.of(
        "producer_byte_rate", 123.0, //should not have decimals
        "consumer_byte_rate", 234.0,  //should not have decimals
        "request_percentage", 10.3 //can have decimal part
    );

    //creating new
    StepVerifier.create(
            quotaService.upsert(cluster, user, clientId, ip, initialQuotas)
        )
        .assertNext(status -> assertThat(status.value()).isEqualTo(201))
        .verifyComplete();

    assertThat(quotaRecordExists(new ClientQuotaRecord(user, clientId, ip, initialQuotas)))
        .isTrue();

    //updating
    StepVerifier.create(
            quotaService.upsert(cluster, user, clientId, ip, Map.of("producer_byte_rate", 22222.0))
        )
        .assertNext(status -> assertThat(status.value()).isEqualTo(200))
        .verifyComplete();

    assertThat(quotaRecordExists(new ClientQuotaRecord(user, clientId, ip, Map.of("producer_byte_rate", 22222.0))))
        .isTrue();

    //deleting created record
    StepVerifier.create(
            quotaService.upsert(cluster, user, clientId, ip, Map.of())
        )
        .assertNext(status -> assertThat(status.value()).isEqualTo(204))
        .verifyComplete();

    assertThat(quotaRecordExists(new ClientQuotaRecord(user, clientId, ip, Map.of("producer_byte_rate", 22222.0))))
        .isFalse();
  }

  private boolean quotaRecordExists(ClientQuotaRecord rec) {
    return Objects.requireNonNull(quotaService.getAll(cluster).collectList().block()).contains(rec);
  }

}
