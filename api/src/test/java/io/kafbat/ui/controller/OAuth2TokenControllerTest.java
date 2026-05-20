package io.kafbat.ui.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.config.auth.OAuthTestSupport;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.test.web.server.MockServerResponseAssertions;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * Tests for OAuth2TokenController - programmatic token endpoint.
 */
class OAuth2TokenControllerTest {

  @AfterAll
  static void stopServers() {
    OAuthTestSupport.stopServers();
  }

  private static final String TEST_CLIENT_ID = "test-client";
  private static final String TEST_CLIENT_SECRET = "test-secret";

  @Nested
  @SpringBootTest(
      classes = {OAuth2TokenController.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithoutProxyInitializer.class)
  @DirtiesContext
  @ActiveProfiles("test")
  class GetToken {

    @Autowired
    private OAuth2TokenController controller;

    @BeforeEach
    void setup() {
      OAuthTestSupport.resetServers();
      OAuthTestSupport.getOAuthServer().stubFor(post(urlPathEqualTo(TOKEN_PATH))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"access_token\":\"programmatic-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
    }

    @Test
    void returnsTokenSuccessfully() {
      StepVerifier.create(controller.getToken(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null))
          .assertNext(response -> {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).containsEntry("access_token", "programmatic-token");
          })
          .verifyComplete();
    }

    @Test
    void returnsTokenWithScope() {
      StepVerifier.create(controller.getToken(TEST_CLIENT_ID, TEST_CLIENT_SECRET, "read write"))
          .assertNext(response -> {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).containsEntry("access_token", "programmatic-token");
          })
          .verifyComplete();
    }
  }
}