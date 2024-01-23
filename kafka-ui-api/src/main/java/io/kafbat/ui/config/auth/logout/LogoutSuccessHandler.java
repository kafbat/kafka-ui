package io.kafbat.ui.config.auth.logout;

import io.kafbat.ui.config.auth.OAuthProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;

public interface LogoutSuccessHandler {

  boolean isApplicable(final String provider);

  Mono<Void> handle(final WebFilterExchange exchange,
                    final Authentication authentication,
                    final OAuthProperties.OAuth2Provider provider);
}
