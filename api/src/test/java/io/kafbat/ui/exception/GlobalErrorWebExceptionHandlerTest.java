package io.kafbat.ui.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

import io.kafbat.ui.model.ErrorResponseDTO;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class GlobalErrorWebExceptionHandlerTest {

  /**
   * Redacts an unexpected error when the property is omitted, preserving other response fields.
   */
  @Test
  void excludesStackTracesByDefault() {
    var response = errorResponse(null, "/unexpected", HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(response.getStackTrace()).isEqualTo("REDACTED FOR SECURITY REASONS");
    assertThat(response.getMessage()).isEqualTo("test failure");
    assertThat(response.getRequestId()).isNotEmpty();
  }

  /**
   * Preserves an explicit request to redact stack traces.
   */
  @Test
  void explicitTrueExcludesStackTraces() {
    var response = errorResponse(true, "/unexpected", HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(response.getStackTrace()).isEqualTo("REDACTED FOR SECURITY REASONS");
  }

  /**
   * Preserves diagnostic stack traces when explicitly enabled.
   */
  @Test
  void explicitFalsePreservesDiagnosticStackTraces() {
    var response = errorResponse(false, "/unexpected", HttpStatus.INTERNAL_SERVER_ERROR);

    assertThat(response.getStackTrace()).contains("java.lang.IllegalStateException: test failure");
  }

  /**
   * Applies default redaction to status errors as well as unexpected errors.
   */
  @Test
  void excludesStackTracesFromStatusErrorsByDefault() {
    var response = errorResponse(null, "/bad-request", HttpStatus.BAD_REQUEST);

    assertThat(response.getStackTrace()).isEqualTo("REDACTED FOR SECURITY REASONS");
    assertThat(response.getMessage()).isEqualTo("invalid request");
  }

  /**
   * Reads an error through a mock WebFlux context without opening a server socket.
   *
   * @param excludeStackTraces explicit property value, or null to test the default
   * @param path fixture route that raises the error
   * @param status expected HTTP response status
   * @return decoded error response
   */
  private ErrorResponseDTO errorResponse(Boolean excludeStackTraces, String path, HttpStatus status) {
    try (var context = new AnnotationConfigApplicationContext()) {
      if (excludeStackTraces != null) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
            Map.of("http.error.excludeStackTraces", excludeStackTraces.toString())));
      }
      context.register(TestConfiguration.class);
      context.refresh();
      return WebTestClient.bindToApplicationContext(context).build()
          .get().uri(path).exchange()
          .expectStatus().isEqualTo(status)
          .expectBody(ErrorResponseDTO.class).returnResult().getResponseBody();
    }
  }

  @Configuration
  @EnableWebFlux
  @Import(GlobalErrorWebExceptionHandler.class)
  static class TestConfiguration {

    /**
     * Resolves the handler's property placeholder against the fixture environment.
     */
    @Bean
    static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    /**
     * Stores fixture exceptions for the actual reactive error handler.
     */
    @Bean
    ErrorAttributes errorAttributes() {
      return new DefaultErrorAttributes();
    }

    /**
     * Supplies unexpected-error and status-error routes for both rendering paths.
     */
    @Bean
    RouterFunction<ServerResponse> routes() {
      return RouterFunctions.route(GET("/unexpected"), request -> Mono.error(new IllegalStateException("test failure")))
          .andRoute(GET("/bad-request"),
              request -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid request")));
    }
  }
}
