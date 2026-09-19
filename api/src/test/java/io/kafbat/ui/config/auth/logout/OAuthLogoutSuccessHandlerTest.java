package io.kafbat.ui.config.auth.logout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.auth.OAuthProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class OAuthLogoutSuccessHandlerTest {

  private static final String REGISTRATION_ID = "test";

  private OAuthProperties properties;
  private ServerLogoutSuccessHandler defaultOidcLogoutHandler;
  private OAuthLogoutSuccessHandler handler;

  @BeforeEach
  void setUp() {
    properties = new OAuthProperties();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setProvider("generic");
    provider.setClientId("test-client");
    properties.getClient().put(REGISTRATION_ID, provider);

    defaultOidcLogoutHandler = mock(ServerLogoutSuccessHandler.class);
    when(defaultOidcLogoutHandler.onLogoutSuccess(any(), any())).thenReturn(Mono.empty());

    handler = new OAuthLogoutSuccessHandler(properties, List.of(), defaultOidcLogoutHandler);
  }

  private WebFilterExchange webFilterExchange() {
    MockServerHttpRequest request = MockServerHttpRequest.get("/logout").build();
    ServerWebExchange serverWebExchange = MockServerWebExchange.from(request);
    return new WebFilterExchange(serverWebExchange, chain -> Mono.empty());
  }

  private OAuth2AuthenticationToken oauth2Token() {
    return new OAuth2AuthenticationToken(
        new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("sub", "user"),
            "sub"),
        List.of(new SimpleGrantedAuthority("ROLE_USER")),
        REGISTRATION_ID);
  }

  @Test
  void shouldRedirectToLogoutPageWhenAuthenticationIsAnonymous() {
    Authentication anonymous = new AnonymousAuthenticationToken(
        "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    WebFilterExchange exchange = webFilterExchange();

    handler.onLogoutSuccess(exchange, anonymous).block();

    assertThat(exchange.getExchange().getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(exchange.getExchange().getResponse().getHeaders().getLocation())
        .isEqualTo(URI.create("/auth?logout"));
  }

  @Test
  void shouldDelegateToDefaultOidcHandlerWhenNoCustomHandlerMatches() {
    WebFilterExchange exchange = webFilterExchange();

    handler.onLogoutSuccess(exchange, oauth2Token()).block();

    verify(defaultOidcLogoutHandler).onLogoutSuccess(any(), any());
  }

  @Test
  void shouldDelegateToCustomHandlerWhenProviderMatches() {
    LogoutSuccessHandler customHandler = mock(LogoutSuccessHandler.class);
    when(customHandler.isApplicable("cognito")).thenReturn(true);
    when(customHandler.handle(any(), any(), any())).thenReturn(Mono.empty());

    OAuthProperties.OAuth2Provider cognitoProvider = new OAuthProperties.OAuth2Provider();
    cognitoProvider.setProvider("cognito");
    cognitoProvider.setClientId("test-client");
    properties.getClient().put(REGISTRATION_ID, cognitoProvider);

    OAuthLogoutSuccessHandler handlerWithCustom =
        new OAuthLogoutSuccessHandler(properties, List.of(customHandler), defaultOidcLogoutHandler);
    WebFilterExchange exchange = webFilterExchange();

    handlerWithCustom.onLogoutSuccess(exchange, oauth2Token()).block();

    verify(customHandler).handle(any(), any(), any());
    verify(defaultOidcLogoutHandler, never()).onLogoutSuccess(any(), any());
  }

  @Test
  void shouldRedirectWhenProviderIdIsMissingFromConfig() {
    OAuthLogoutSuccessHandler handlerWithoutProvider =
        new OAuthLogoutSuccessHandler(new OAuthProperties(), List.of(), defaultOidcLogoutHandler);
    WebFilterExchange exchange = webFilterExchange();

    handlerWithoutProvider.onLogoutSuccess(exchange, oauth2Token()).block();

    assertThat(exchange.getExchange().getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
  }
}
