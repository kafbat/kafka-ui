package io.kafbat.ui.config.auth;

abstract class AbstractAuthSecurityConfig {

  protected AbstractAuthSecurityConfig() {

  }

  protected static final String[] AUTH_WHITELIST = {
      /* STATIC */
      "/index.html",
      "/assets/**",
      "/manifest.json",
      "/favicon.svg",
      "/favicon/**",

      "/static/**",
      "/resources/**",

      /* ACTUATOR */
      "/actuator/health/**",
      "/actuator/info",
      "/actuator/prometheus",

      /* AUTH */
      "/login",
      "/logout",
      "/oauth2/**",
      "/api/config/authentication",
      "/api/authorization"
  };

}
