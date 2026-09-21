package io.kafbat.ui.config.auth.logout;

import io.kafbat.ui.config.auth.OAuthSecurityConfig;
import io.kafbat.ui.config.auth.OAuthTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebHandler;

/**
 * Integration test for the OAuth2 logout flow through the real security filter chain.
 *
 * <p>Reproduces the scenario from issue #1960: an anonymous session hits {@code POST /logout}
 * (the path is whitelisted), reaches {@link OAuthLogoutSuccessHandler} and must be redirected
 * to {@code /auth?logout} instead of failing with a {@code ClassCastException}.
 */
@SpringBootTest(
    classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class,
        OAuthLogoutIntegrationTest.WebHandlerConfig.class},
    properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
@ContextConfiguration(initializers = OAuthTestSupport.WithoutProxyInitializer.class)
@DirtiesContext
@ActiveProfiles("test")
class OAuthLogoutIntegrationTest {

  @AfterAll
  static void stopServers() {
    OAuthTestSupport.stopServers();
  }

  @Autowired
  ApplicationContext context;

  @Test
  void anonymousLogoutRedirectsToAuthLogoutInsteadOf500() {
    WebTestClient.bindToApplicationContext(context).build()
        .post()
        .uri("/logout")
        .exchange()
        .expectStatus()
        .isFound()
        .expectHeader()
        .valueEquals("Location", "/auth?logout");
  }

  /**
   * Minimal reactive web setup for {@link WebTestClient#bindToApplicationContext}.
   * The security filter chain intercepts {@code /logout} before this handler is reached.
   */
  @TestConfiguration
  static class WebHandlerConfig {

    @Bean
    public WebHandler webHandler(ApplicationContext context) {
      return new DispatcherHandler(context);
    }
  }
}
