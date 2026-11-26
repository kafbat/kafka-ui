package io.kafbat.ui.config.auth.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.kafbat.ui.config.auth.OAuthProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Mono;

class OAuthLogoutFallbackTest {

  @Test
  void nonOAuth2AuthenticationShortCircuitsBeforeProviderLookup() {
    ServerLogoutSuccessHandler defaultOidcLogoutHandler = mock(ServerLogoutSuccessHandler.class);
    OAuthLogoutSuccessHandler handler = new OAuthLogoutSuccessHandler(
        new OAuthProperties(), List.of(), defaultOidcLogoutHandler
    );
    MockServerWebExchange serverWebExchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/logout").build()
    );
    WebFilterExchange exchange = new WebFilterExchange(serverWebExchange, chain -> Mono.empty());
    Authentication authentication = new UsernamePasswordAuthenticationToken("user", "password", List.of());

    handler.onLogoutSuccess(exchange, authentication).block();

    assertThat(serverWebExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(serverWebExchange.getResponse().getHeaders().getLocation()).hasToString("/login?logout");
    verifyNoInteractions(defaultOidcLogoutHandler);
  }
}
