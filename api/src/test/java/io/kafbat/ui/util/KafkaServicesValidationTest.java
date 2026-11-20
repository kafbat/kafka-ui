package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.TextNode;
import io.kafbat.ui.model.ApplicationPropertyValidationDTO;
import io.kafbat.ui.service.ksql.KsqlApiClient;
import io.kafbat.ui.sr.api.KafkaSrClientApi;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class KafkaServicesValidationTest {

  private static final String REMOTE_DETAILS = "remote service response details";

  @Test
  void validatorsReturnFixedMessageOnClientError() {
    Stream.of(
            KafkaServicesValidation.validateSchemaRegistry(this::failingClient),
            KafkaServicesValidation.validateConnect(this::failingClient),
            KafkaServicesValidation.validateKsql(this::failingClient),
            KafkaServicesValidation.validatePrometheusStore(this::failingClient))
        .forEach(result -> assertFixedError(result, "Error validating service. See logs for details."));
  }

  @Test
  @SuppressWarnings("unchecked")
  void validateKsqlReturnsFixedMessageOnErrorResponseTable() {
    ReactiveFailover<KsqlApiClient> client = mock(ReactiveFailover.class);
    when(client.flux(any())).thenReturn(Flux.just(
        KsqlApiClient.KsqlResponseTable.builder()
            .header("Execution error")
            .columnNames(List.of("message"))
            .values(List.of(List.of(new TextNode(REMOTE_DETAILS))))
            .error(true)
            .build()));

    assertFixedError(KafkaServicesValidation.validateKsql(() -> client),
        "Error response from ksql. See logs for details.");
  }

  @Test
  void validatorsReturnFixedMessageWhenClientCreationFails() {
    Supplier<ReactiveFailover<KafkaSrClientApi>> failingSupplier = () -> {
      throw new IllegalStateException(REMOTE_DETAILS);
    };

    assertFixedError(KafkaServicesValidation.validateSchemaRegistry(failingSupplier),
        "Error creating Schema Registry client. See logs for details.");
  }

  @SuppressWarnings("unchecked")
  private <T> ReactiveFailover<T> failingClient() {
    ReactiveFailover<T> client = mock(ReactiveFailover.class);
    when(client.mono(any())).thenReturn(Mono.error(new RuntimeException(REMOTE_DETAILS)));
    when(client.flux(any())).thenReturn(Flux.error(new RuntimeException(REMOTE_DETAILS)));
    return client;
  }

  private static void assertFixedError(Mono<ApplicationPropertyValidationDTO> result, String expectedMessage) {
    StepVerifier.create(result)
        .assertNext(dto -> {
          assertThat(dto.getError()).isTrue();
          assertThat(dto.getErrorMessage()).isEqualTo(expectedMessage);
        })
        .verifyComplete();
  }
}
