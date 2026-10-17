package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.kafbat.ui.model.CompatibilityLevelDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.sr.ApiClient;
import io.kafbat.ui.sr.api.KafkaSrClientApi;
import io.kafbat.ui.util.ReactiveFailover;
import io.kafbat.ui.util.WebClientConfigurator;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SchemaRegistryCustomCompatibilityTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  private final MockWebServer registry = new MockWebServer();
  private SchemaRegistryService service;
  private KafkaCluster cluster;

  @BeforeEach
  void setUp() throws IOException {
    registry.start();
    var webClient = new WebClientConfigurator()
        .configureAdditionalDecoderMediaTypes(
            MediaType.parseMediaType("application/vnd.schemaregistry.v1+json"))
        .build();
    var client = new ApiClient(webClient).setBasePath(registry.url("/").toString());
    cluster = mock(KafkaCluster.class);
    when(cluster.getSchemaRegistryClient())
        .thenReturn(ReactiveFailover.createNoop(new KafkaSrClientApi(client)));
    service = new SchemaRegistryService(mock(StatisticsCache.class));
  }

  @AfterEach
  void tearDown() throws IOException {
    registry.close();
  }

  private void respond(String json) {
    registry.enqueue(new MockResponse()
        .setHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        .setBody(json));
  }

  @Test
  void readsCustomGlobalCompatibilityAndMapsToUi() {
    respond("{\"compatibilityLevel\":\"CUSTOM_MODE\"}");
    var level = service.getGlobalSchemaCompatibilityLevel(cluster).block(TIMEOUT);
    assertThat(level).isEqualTo("CUSTOM_MODE");
  }

  @Test
  void readsCustomSubjectCompatibilityWithoutDiscardingIt() {
    respond("{\"compatibilityLevel\":\"CUSTOM_MODE\"}");
    // An unknown enum used to be swallowed here, triggering the global BACKWARD fallback.
    assertThat(service.getSchemaCompatibilityLevel(cluster, "orders-value").block(TIMEOUT))
        .isEqualTo("CUSTOM_MODE");
    assertThat(registry.getRequestCount()).isEqualTo(1);
  }

  @Test
  void sendsCustomSubjectUpdateFromUiDto() throws Exception {
    respond("{\"compatibility\":\"CUSTOM_MODE\"}");
    var dto = new JsonMapper().readValue("{\"compatibility\":\"CUSTOM_MODE\"}", CompatibilityLevelDTO.class);
    service.updateSchemaCompatibility(cluster, "orders-value", dto.getCompatibility())
        .block(TIMEOUT);
    var request = registry.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getMethod()).isEqualTo("PUT");
    assertThat(request.getPath()).endsWith("/config/orders-value");
    assertThat(new JsonMapper().readTree(request.getBody().readUtf8()).get("compatibility").asText())
        .isEqualTo("CUSTOM_MODE");
  }

  @Test
  void sendsCustomGlobalUpdate() throws Exception {
    respond("{\"compatibility\":\"CUSTOM_MODE\"}");
    service.updateGlobalSchemaCompatibility(cluster, "CUSTOM_MODE").block(TIMEOUT);
    var request = registry.takeRequest(5, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getMethod()).isEqualTo("PUT");
    assertThat(request.getPath()).endsWith("/config");
    assertThat(new JsonMapper().readTree(request.getBody().readUtf8()).get("compatibility").asText())
        .isEqualTo("CUSTOM_MODE");
  }

  @Test
  void standardModesStillRoundTrip() {
    respond("{\"compatibilityLevel\":\"BACKWARD\"}");
    assertThat(service.getGlobalSchemaCompatibilityLevel(cluster).block(TIMEOUT))
        .isEqualTo("BACKWARD");
  }
}
