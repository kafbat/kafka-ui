package io.kafbat.ui.config.auth;

abstract class AbstractAuthSecurityConfig {

  protected AbstractAuthSecurityConfig() {

  }

  protected static final String[] AUTH_WHITELIST = {
      "/css/**",
      "/js/**",
      "/media/**",
      "/resources/**",
      "/actuator/health/**",
      "/actuator/info",
      "/actuator/prometheus",
      "/auth",
      "/login",
      "/logout",
      "/oauth2/**",
      "/static/**",
      "/api/config/authentication",
      "/index.html",
      "/assets/**",
      "/manifest.json",
      "/favicon/**",
      "/api/authorization"
  };

}
