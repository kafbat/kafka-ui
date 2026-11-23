package io.kafbat.ui.config.auth.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.kafbat.ui.config.auth.OAuthProperties;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;

class CognitoLogoutSuccessHandlerTest {

  private CognitoLogoutSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CognitoLogoutSuccessHandler();
  }

  @Test
  void shouldHandleWithNegativePortBehindProxy() {
    MockServerHttpRequest request = MockServerHttpRequest.get("https://proxy.kafbat-ui.com/ui/clusters").build();
    ServerWebExchange serverWebExchange = MockServerWebExchange.from(request);
    WebFilterExchange exchange = new WebFilterExchange(serverWebExchange, chain -> reactor.core.publisher.Mono.empty());
    Authentication authentication = mock(Authentication.class);

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setClientId("my-client-id");
    provider.setCustomParams(Map.of("logoutUrl", "https://auth.cognito.com/logout"));

    handler.handle(exchange, authentication, provider).block();

    assertThat(serverWebExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
    URI location = serverWebExchange.getResponse().getHeaders().getLocation();
    assertThat(location).isNotNull();

    assertThat(location.toString()).isEqualTo(
        "https://auth.cognito.com/logout?client_id=my-client-id&logout_uri=https://proxy.kafbat-ui.com/"
    );
  }

  @Test
  void shouldHandleWithExplicitPort() {
    MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost:8080/ui/clusters").build();
    ServerWebExchange serverWebExchange = MockServerWebExchange.from(request);
    WebFilterExchange exchange = new WebFilterExchange(serverWebExchange, chain -> reactor.core.publisher.Mono.empty());
    Authentication authentication = mock(Authentication.class);

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setClientId("test-client");
    provider.setCustomParams(Map.of("logoutUrl", "https://auth.cognito.com/logout"));

    handler.handle(exchange, authentication, provider).block();

    assertThat(serverWebExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
    URI location = serverWebExchange.getResponse().getHeaders().getLocation();
    assertThat(location).isNotNull();

    assertThat(location.toString()).isEqualTo(
        "https://auth.cognito.com/logout?client_id=test-client&logout_uri=http://localhost:8080/"
    );
  }
}
