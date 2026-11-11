package io.kafbat.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class StringJackson2JsonEncoderTest {

  private final WebTestClient client = WebTestClient.bindToController(new TestController())
      .httpMessageCodecs(new CodecConfig().stringJsonEncoderCustomizer(new ObjectMapper())::customize)
      .build();

  @Test
  void fluxOfStringsIsWrittenAsJsonArray() {
    client.get().uri("/flux")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(String.class).isEqualTo("[\"a\",\"b\"]");
  }

  @Test
  void fluxOfStringsDecodesAsList() {
    client.get().uri("/flux")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectBody(new ParameterizedTypeReference<List<String>>() {
        })
        .isEqualTo(List.of("a", "b"));
  }

  @Test
  void emptyFluxIsWrittenAsEmptyArray() {
    client.get().uri("/empty")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectBody(String.class).isEqualTo("[]");
  }

  @Test
  void monoOfStringIsWrittenAsIs() {
    client.get().uri("/mono")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(String.class).isEqualTo("{\"raw\":true}");
  }

  @Test
  void monoOfStringWithAnyAcceptStaysPlainText() {
    client.get().uri("/mono")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
        .expectBody(String.class).isEqualTo("{\"raw\":true}");
  }

  @Test
  void fluxOfStringsAsPlainTextIsUnchanged() {
    client.get().uri("/flux")
        .accept(MediaType.TEXT_PLAIN)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo("ab");
  }

  @Test
  void fluxOfStringsWithNonAsciiInUtf8() {
    client.get().uri("/accented")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class).isEqualTo("[\"café\"]");
  }

  @Test
  void fluxOfStringsUsesRequestedNonUnicodeCharset() {
    MediaType latin1 = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.ISO_8859_1);
    client.get().uri("/accented")
        .accept(latin1)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(latin1)
        .expectBody(byte[].class).isEqualTo("[\"caf\\u00E9\"]".getBytes(StandardCharsets.ISO_8859_1));
  }

  @RestController
  static class TestController {

    @GetMapping("/flux")
    Flux<String> flux() {
      return Flux.just("a", "b");
    }

    @GetMapping("/empty")
    Flux<String> empty() {
      return Flux.empty();
    }

    @GetMapping("/accented")
    Flux<String> accented() {
      return Flux.just("café");
    }

    @GetMapping("/mono")
    Mono<String> mono() {
      return Mono.just("{\"raw\":true}");
    }
  }
}
