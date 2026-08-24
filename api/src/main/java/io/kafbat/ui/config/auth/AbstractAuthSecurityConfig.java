package io.kafbat.ui.config.auth;

import io.kafbat.ui.util.EmptyRedirectStrategy;
import java.net.URI;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

abstract class AbstractAuthSecurityConfig {

  protected AbstractAuthSecurityConfig() {

  }

  protected static final String LOGIN_URL = "/login";
  protected static final String LOGOUT_URL = "/auth?logout";

  protected static final String[] AUTH_WHITELIST = {
      /* STATIC */
      "/index.html",
      "/assets/**",
      "/manifest.json",
      "/favicon.svg",
      "/favicon/**",
      "/images/**",
      "/fonts/**",

      "/static/**",
      "/resources/**",
      "/metrics",

      /* ACTUATOR */
      "/actuator/health/**",
      "/actuator/info",
      "/actuator/prometheus",

      /* SWAGGER UI */
      "/swagger-ui/**",
      "/v3/api-docs",
      "/v3/api-docs/**",
      "/webjars/**",

      /* AUTH */
      "/login",
      "/logout",
      "/oauth2/**",
      "/api/config/authentication",
      "/api/authorization"
  };

  protected RedirectServerAuthenticationSuccessHandler emptyRedirectSuccessHandler() {
    final var authHandler = new RedirectServerAuthenticationSuccessHandler();
    authHandler.setRedirectStrategy(new EmptyRedirectStrategy());
    return authHandler;
  }

  protected RedirectServerLogoutSuccessHandler redirectLogoutSuccessHandler() {
    final var logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
    logoutSuccessHandler.setLogoutSuccessUrl(URI.create(LOGOUT_URL));
    return logoutSuccessHandler;
  }

}
