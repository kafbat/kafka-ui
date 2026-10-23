package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.web.server.SecurityWebFilterChain;

class OAuthSecurityConfigStartupTest {

  private final ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
      .withUserConfiguration(OAuthSecurityConfig.class, OAuthLogoutSuccessHandler.class, AccessControlService.class)
      .withPropertyValues(
          "auth.type=OAUTH2",
          "rbac.roles[0].name=viewer",
          "rbac.roles[0].clusters[0]=local",
          "rbac.roles[0].subjects[0].provider=oauth",
          "rbac.roles[0].subjects[0].type=role",
          "rbac.roles[0].subjects[0].value=viewer",
          "rbac.roles[0].permissions[0].resource=topic",
          "rbac.roles[0].permissions[0].value=.*",
          "rbac.roles[0].permissions[0].actions[0]=view");

  @Test
  void startsAsJwtResourceServerOnly() {
    runner
        .withPropertyValues("auth.oauth2.resource-server.jwt.jwk-set-uri=http://localhost/jwks")
        .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SecurityWebFilterChain.class));
  }

  @Test
  void startsAsOpaqueTokenResourceServerOnly() {
    runner
        .withPropertyValues(
            "auth.oauth2.resource-server.opaquetoken.introspection-uri=http://localhost/introspect",
            "auth.oauth2.resource-server.opaquetoken.client-id=client",
            "auth.oauth2.resource-server.opaquetoken.client-secret=secret")
        .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SecurityWebFilterChain.class));
  }

  @Test
  void startsWithClientRegistrations() {
    runner
        .withPropertyValues(
            "auth.oauth2.client.test.provider=test",
            "auth.oauth2.client.test.client-id=client",
            "auth.oauth2.client.test.client-secret=secret",
            "auth.oauth2.client.test.authorization-grant-type=authorization_code",
            "auth.oauth2.client.test.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
            "auth.oauth2.client.test.scope=openid",
            "auth.oauth2.client.test.authorization-uri=http://localhost/authorize",
            "auth.oauth2.client.test.token-uri=http://localhost/token",
            "auth.oauth2.client.test.user-info-uri=http://localhost/userinfo",
            "auth.oauth2.client.test.jwk-set-uri=http://localhost/jwks",
            "auth.oauth2.client.test.user-name-attribute=sub")
        .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SecurityWebFilterChain.class));
  }

  @Test
  void failsWithoutProviders() {
    runner.run(context -> assertThat(context).getFailure()
        .rootCause().hasMessage("OAuth2 authentication is enabled but no providers specified."));
  }

  @Test
  void failsWithResourceServerWithoutUri() {
    runner
        .withPropertyValues("auth.oauth2.resource-server.jwt.issuer-uri=http://localhost")
        .run(context -> assertThat(context).getFailure()
            .rootCause().hasMessage("OAuth2 authentication is enabled but no providers specified."));
  }
}
