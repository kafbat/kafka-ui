package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientConfiguratorTest {

  private final MockWebServer mockWebServer = new MockWebServer();

  @BeforeEach
  void startMockServer() throws IOException {
    mockWebServer.start();
  }

  @AfterEach
  void stopMockServer() throws IOException {
    mockWebServer.close();
  }

  @Test
  void decodesStandardApplicationJson() {
    mockWebServer.enqueue(new MockResponse()
        .addHeader("Content-Type", "application/json")
        .setBody("{\"name\":\"test\"}"));

    WebClient client = new WebClientConfigurator().build();
    String body = client.get()
        .uri(mockWebServer.url("/").toString())
        .retrieve()
        .bodyToMono(String.class)
        .block();

    assertThat(body).isEqualTo("{\"name\":\"test\"}");
  }

  @Test
  void decodesVendorMediaTypeWhenConfigured() {
    MediaType srV1Json = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json");

    mockWebServer.enqueue(new MockResponse()
        .addHeader("Content-Type", "application/vnd.schemaregistry.v1+json")
        .setBody("{\"compatibilityLevel\":\"BACKWARD\"}"));

    WebClient client = new WebClientConfigurator()
        .configureAdditionalDecoderMediaTypes(srV1Json)
        .build();

    CompatibilityConfig config = client.get()
        .uri(mockWebServer.url("/config").toString())
        .retrieve()
        .bodyToMono(CompatibilityConfig.class)
        .block();

    assertThat(config).isNotNull();
    assertThat(config.compatibilityLevel()).isEqualTo("BACKWARD");
  }

  @Test
  void decodesMultipleVendorMediaTypes() {
    MediaType srV1Json = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json");
    MediaType srJson = MediaType.parseMediaType("application/vnd.schemaregistry+json");

    mockWebServer.enqueue(new MockResponse()
        .addHeader("Content-Type", "application/vnd.schemaregistry+json")
        .setBody("{\"compatibilityLevel\":\"FULL\"}"));

    WebClient client = new WebClientConfigurator()
        .configureAdditionalDecoderMediaTypes(srV1Json, srJson)
        .build();

    CompatibilityConfig config = client.get()
        .uri(mockWebServer.url("/config").toString())
        .retrieve()
        .bodyToMono(CompatibilityConfig.class)
        .block();

    assertThat(config).isNotNull();
    assertThat(config.compatibilityLevel()).isEqualTo("FULL");
  }

  record CompatibilityConfig(String compatibilityLevel) {
  }
}
