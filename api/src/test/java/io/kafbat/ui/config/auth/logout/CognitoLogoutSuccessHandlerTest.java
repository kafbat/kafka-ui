package io.kafbat.ui.config.auth.logout;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.auth.OAuthProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class CognitoLogoutSuccessHandlerTest {

  private final CognitoLogoutSuccessHandler handler = new CognitoLogoutSuccessHandler();

  @Test
  void buildsLogoutRedirectWhenRequestUriHasNoExplicitPort() {
    // Behind a reverse proxy (e.g. AWS ALB) with SSL termination, the resolved
    // request URI has no explicit port, so java.net.URI#getPort() returns -1.
    // See https://github.com/kafbat/kafka-ui/issues/1878
    var request = MockServerHttpRequest
        .post("https://kafka-ui.example.com/logout")
        .build();
    var exchange = MockServerWebExchange.from(request);
    var filterExchange = new WebFilterExchange(exchange, noopChain());

    var provider = new OAuthProperties.OAuth2Provider();
    provider.setClientId("test-client-id");
    provider.setCustomParams(Map.of("logoutUrl",
        "https://cognito-domain.auth.us-east-1.amazoncognito.com/logout"));

    handler.handle(filterExchange, new TestingAuthenticationToken("user", "pass"), provider)
        .block();

    var location = exchange.getResponse().getHeaders().getLocation();
    assertThat(location).isNotNull();
    assertThat(location.toString()).doesNotContain(":-1");
    assertThat(location.getHost()).isEqualTo("cognito-domain.auth.us-east-1.amazoncognito.com");
    assertThat(location.getQuery()).contains("client_id=test-client-id");
    assertThat(location.getQuery()).contains("logout_uri=https");
  }

  private WebFilterChain noopChain() {
    return ex -> Mono.empty();
  }
}
