package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class EmptyRedirectStrategyTest {

  private static final EmptyRedirectStrategy STRATEGY = new EmptyRedirectStrategy();

  @Test
  void prependsContextPathToRootRedirect() {
    var exchange = exchangeWithContextPath("/kafka");

    StepVerifier.create(STRATEGY.sendRedirect(exchange, URI.create("/")))
        .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/kafka/"));
  }

  @Test
  void prependsContextPathToAbsolutePathRedirect() {
    var exchange = exchangeWithContextPath("/kafka");

    StepVerifier.create(STRATEGY.sendRedirect(exchange, URI.create("/ui/clusters")))
        .verifyComplete();

    assertThat(exchange.getResponse().getHeaders().getLocation())
        .isEqualTo(URI.create("/kafka/ui/clusters"));
  }

  @Test
  void leavesLocationUntouchedWhenNoContextPath() {
    var exchange = exchangeWithContextPath("");

    StepVerifier.create(STRATEGY.sendRedirect(exchange, URI.create("/")))
        .verifyComplete();

    assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/"));
  }

  @Test
  void doesNotRewriteAbsoluteUrlRedirects() {
    var exchange = exchangeWithContextPath("/kafka");
    var external = URI.create("https://auth.example.com/login");

    StepVerifier.create(STRATEGY.sendRedirect(exchange, external))
        .verifyComplete();

    assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(external);
  }

  private static MockServerWebExchange exchangeWithContextPath(String contextPath) {
    return MockServerWebExchange.from(
        MockServerHttpRequest.get(contextPath + "/").contextPath(contextPath));
  }
}
