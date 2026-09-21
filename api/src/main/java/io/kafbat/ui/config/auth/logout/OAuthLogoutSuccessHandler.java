package io.kafbat.ui.config.auth.logout;

import io.kafbat.ui.config.auth.OAuthProperties;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(value = "auth.type", havingValue = "OAUTH2")
public class OAuthLogoutSuccessHandler implements ServerLogoutSuccessHandler {

  private static final String LOGOUT_URL = "/auth?logout";

  private final OAuthProperties properties;
  private final List<LogoutSuccessHandler> logoutSuccessHandlers;
  private final ServerLogoutSuccessHandler defaultOidcLogoutHandler;

  public OAuthLogoutSuccessHandler(final OAuthProperties properties,
                                   final List<LogoutSuccessHandler> logoutSuccessHandlers,
                                   final @Qualifier("defaultOidcLogoutHandler") ServerLogoutSuccessHandler handler) {
    this.properties = properties;
    this.logoutSuccessHandlers = logoutSuccessHandlers;
    this.defaultOidcLogoutHandler = handler;
  }

  @Override
  public Mono<Void> onLogoutSuccess(final WebFilterExchange exchange,
                                    final Authentication authentication) {
    if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
      // Session is anonymous/expired — nothing to clean up. Redirect to the
      // logout page instead of failing with a ClassCastException.
      return redirectToLogoutPage(exchange);
    }
    final String providerId = oauthToken.getAuthorizedClientRegistrationId();
    final OAuthProperties.OAuth2Provider oAuth2Provider = properties.getClient().get(providerId);
    if (oAuth2Provider == null) {
      // Provider was removed from the config while the session was alive.
      return redirectToLogoutPage(exchange);
    }
    return getLogoutHandler(oAuth2Provider.getProvider())
        .map(handler -> handler.handle(exchange, authentication, oAuth2Provider))
        .orElseGet(() -> defaultOidcLogoutHandler.onLogoutSuccess(exchange, authentication));
  }

  private Mono<Void> redirectToLogoutPage(final WebFilterExchange exchange) {
    final ServerHttpResponse response = exchange.getExchange().getResponse();
    response.setStatusCode(HttpStatus.FOUND);
    response.getHeaders().setLocation(URI.create(LOGOUT_URL));
    return Mono.empty();
  }

  private Optional<LogoutSuccessHandler> getLogoutHandler(final String provider) {
    return logoutSuccessHandlers.stream()
        .filter(h -> h.isApplicable(provider))
        .findFirst();
  }
}
