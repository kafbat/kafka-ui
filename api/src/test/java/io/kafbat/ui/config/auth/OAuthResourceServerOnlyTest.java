package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that {@code auth.type=OAUTH2} can start with only
 * {@code auth.oauth2.resource-server} configured and zero {@code auth.oauth2.client.*}
 * registrations — a valid bearer/JWT-only (machine-to-machine) setup that previously
 * failed at startup with "OAuth2 authentication is enabled but no providers specified."
 *
 * <p>{@link OAuthLogoutSuccessHandler} is a plain {@code @Component} (not a {@code @Bean}
 * declared inside {@link OAuthSecurityConfig}), so it must be listed explicitly here —
 * a bare {@code @SpringBootTest(classes = ...)} slice doesn't component-scan.
 *
 * @see <a href="https://github.com/kafbat/kafka-ui/issues/1929">#1929</a>
 */
@SpringBootTest(
    classes = {
        OAuthSecurityConfig.class,
        OAuthLogoutSuccessHandler.class,
        OAuthResourceServerOnlyTest.NoClientTestConfig.class
    },
    properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "auth.type=OAUTH2",
        "auth.oauth2.resource-server.jwt.jwk-set-uri=http://localhost/.well-known/jwks.json"
    })
@ActiveProfiles("test")
class OAuthResourceServerOnlyTest {

  @Autowired
  private SecurityWebFilterChain filterChain;
  @Autowired
  private ObjectProvider<ReactiveClientRegistrationRepository> repository;

  @Test
  void contextLoadsWithoutClientRegistrations() {
    assertThat(filterChain).isNotNull();
    // no auth.oauth2.client.* configured -> the bean is legitimately absent
    assertThat(repository.getIfAvailable()).isNull();
  }

  @TestConfiguration
  static class NoClientTestConfig {

    @Bean
    @Primary
    AccessControlService accessControlService() {
      AccessControlService acs = mock(AccessControlService.class);
      when(acs.getOauthExtractors()).thenReturn(java.util.Collections.emptySet());
      return acs;
    }
  }
}
