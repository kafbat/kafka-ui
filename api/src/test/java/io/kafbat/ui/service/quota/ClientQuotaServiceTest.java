package io.kafbat.ui.service.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.service.ClustersStorage;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.assertj.core.api.ListAssert;
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

    awaitAndVerify((l) ->
        l.contains(new ClientQuotaRecord(user, clientId, ip, initialQuotas))
    );

    //updating
    StepVerifier.create(
            quotaService.upsert(cluster, user, clientId, ip, Map.of("producer_byte_rate", 22222.0))
        )
        .assertNext(status -> assertThat(status.value()).isEqualTo(200))
        .verifyComplete();

    awaitAndVerify((l) ->
        l.contains(new ClientQuotaRecord(user, clientId, ip, Map.of("producer_byte_rate", 22222.0)))
    );

    //deleting created record
    StepVerifier.create(
            quotaService.upsert(cluster, user, clientId, ip, Map.of())
        )
        .assertNext(status -> assertThat(status.value()).isEqualTo(204))
        .verifyComplete();

    awaitAndVerify((l) ->
        l.doesNotContain(new ClientQuotaRecord(user, clientId, ip, Map.of("producer_byte_rate", 22222.0)))
    );
  }

  private void awaitAndVerify(Consumer<ListAssert<ClientQuotaRecord>> verifier) {
    await()
        .atMost(5, TimeUnit.SECONDS)
        .pollInterval(200, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> verifier.accept(assertThat(quotaService.getAll(cluster).collectList().block())));
  }

}
