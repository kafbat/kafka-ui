package io.kafbat.ui.client;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import java.io.IOException;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

class RetryingKafkaConnectClientTest {

  private final MockWebServer mockWebServer = new MockWebServer();

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.close();
  }

  private RetryingKafkaConnectClient createClient(ClustersProperties.KafkaConnect connectConfig) {
    var connectCluster = ClustersProperties.ConnectCluster.builder()
        .name("test-connect")
        .address(mockWebServer.url("/").toString())
        .build();
    return new RetryingKafkaConnectClient(
        connectCluster,
        null,
        DataSize.ofMegabytes(20),
        Duration.ofSeconds(5),
        connectConfig
    );
  }

  private ClustersProperties.KafkaConnect fastRetryConfig() {
    var config = new ClustersProperties.KafkaConnect();
    config.setMaxRetries(3);
    config.setRetryBaseDelayMs(50);
    config.setRetryMaxDelayMs(200);
    return config;
  }

  @Test
  void retriesOn502AndEventuallySucceeds() {
    var client = createClient(fastRetryConfig());

    // First two calls return 502, third returns 200 with valid connectors JSON
    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{}"));

    StepVerifier.create(client.getConnectors(null, null))
        .assertNext(result -> assertThat(result).isEmpty())
        .verifyComplete();

    assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
  }

  @Test
  void retriesOn503AndEventuallySucceeds() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service Unavailable"));
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{}"));

    StepVerifier.create(client.getConnectors(null, null))
        .assertNext(result -> assertThat(result).isEmpty())
        .verifyComplete();

    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void retriesOn429AndEventuallySucceeds() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("Too Many Requests"));
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{}"));

    StepVerifier.create(client.getConnectors(null, null))
        .assertNext(result -> assertThat(result).isEmpty())
        .verifyComplete();

    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }

  @Test
  void failsAfterMaxRetriesExhausted() {
    var client = createClient(fastRetryConfig());

    // Enqueue more 502s than maxRetries (3 retries + 1 initial = 4 attempts total)
    for (int i = 0; i < 5; i++) {
      mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    }

    StepVerifier.create(client.getConnectors(null, null))
        .expectErrorSatisfies(e -> {
          assertThat(e).isInstanceOf(WebClientResponseException.BadGateway.class);
          assertThat(e.getMessage()).contains("502 Bad Gateway");
        })
        .verify(Duration.ofSeconds(10));

    // 1 initial + 3 retries = 4 total
    assertThat(mockWebServer.getRequestCount()).isEqualTo(4);
  }

  @Test
  void doesNotRetryOn400() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Bad Request"));

    StepVerifier.create(client.getConnectors(null, null))
        .expectError(WebClientResponseException.BadRequest.class)
        .verify(Duration.ofSeconds(5));

    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  void doesNotRetryOn404() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not Found"));

    StepVerifier.create(client.getConnectors(null, null))
        .expectError(WebClientResponseException.NotFound.class)
        .verify(Duration.ofSeconds(5));

    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  @Test
  void retriesWithExponentialBackoff() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{}"));

    long start = System.currentTimeMillis();
    StepVerifier.create(client.getConnectors(null, null))
        .assertNext(result -> assertThat(result).isEmpty())
        .verifyComplete();
    long elapsed = System.currentTimeMillis() - start;

    // With baseDelay=50ms and 2 retries, total should be at least ~50ms (backoff applies)
    assertThat(elapsed).isGreaterThanOrEqualTo(50);
  }

  @Test
  void getConnectorTopicsRetriesOnTransientError() {
    var client = createClient(fastRetryConfig());

    mockWebServer.enqueue(new MockResponse().setResponseCode(502).setBody("Bad Gateway"));
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody("{\"test-connector\": {\"topics\": [\"topic1\"]}}"));

    StepVerifier.create(client.getConnectorTopics("test-connector"))
        .assertNext(result -> {
          assertThat(result).containsKey("test-connector");
          assertThat(result.get("test-connector").getTopics()).containsExactly("topic1");
        })
        .verifyComplete();

    assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
  }
}
