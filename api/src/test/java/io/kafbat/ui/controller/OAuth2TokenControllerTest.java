package io.kafbat.ui.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.kafbat.ui.config.auth.OAuthTestSupport.TOKEN_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.config.auth.OAuthTestSupport;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
  private static final String VALID_TOKEN_RESPONSE =
      "{\"access_token\":\"programmatic-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}";

  private static OAuth2TokenController.TokenRequest tokenRequest(String clientId, String clientSecret, String scope) {
    var req = new OAuth2TokenController.TokenRequest();
    req.setClientId(clientId);
    req.setClientSecret(clientSecret);
    req.setScope(scope);
    return req;
  }

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
              .withBody(VALID_TOKEN_RESPONSE)));
    }

    @Test
    void returnsTokenSuccessfully() {
      StepVerifier.create(controller.getToken(tokenRequest(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null)))
          .assertNext(response -> {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).containsEntry("access_token", "programmatic-token");
          })
          .verifyComplete();
    }

    @Test
    void returnsTokenWithScope() {
      StepVerifier.create(controller.getToken(tokenRequest(TEST_CLIENT_ID, TEST_CLIENT_SECRET, "read write")))
          .assertNext(response -> {
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).containsEntry("access_token", "programmatic-token");
          })
          .verifyComplete();
    }

    @Test
    void returns401ForInvalidCredentials() {
      StepVerifier.create(controller.getToken(tokenRequest(TEST_CLIENT_ID, "wrong-secret", null)))
          .assertNext(response -> {
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).containsEntry("error", "authentication_failed");
            // Verify no internal exception message leaks to client
            assertThat(response.getBody().get("message")).isEqualTo("Authentication failed");
          })
          .verifyComplete();
    }

    @Test
    void returns401WhenUpstreamReturns4xx() {
      OAuthTestSupport.getOAuthServer().stubFor(post(urlPathEqualTo(TOKEN_PATH))
          .willReturn(aResponse()
              .withStatus(400)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"error\":\"invalid_client\",\"error_description\":\"Client authentication failed\"}")));

      StepVerifier.create(controller.getToken(tokenRequest(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null)))
          .assertNext(response -> {
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).containsEntry("error", "authentication_failed");
            assertThat(response.getBody().get("message")).isEqualTo("Authentication failed");
          })
          .verifyComplete();
    }

    @Test
    void returns401WhenUpstreamReturns5xx() {
      OAuthTestSupport.getOAuthServer().stubFor(post(urlPathEqualTo(TOKEN_PATH))
          .willReturn(aResponse()
              .withStatus(502)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"error\":\"server_error\"}")));

      StepVerifier.create(controller.getToken(tokenRequest(TEST_CLIENT_ID, TEST_CLIENT_SECRET, null)))
          .assertNext(response -> {
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).containsEntry("error", "authentication_failed");
            assertThat(response.getBody().get("message")).isEqualTo("Authentication failed");
          })
          .verifyComplete();
    }

    @Test
    void returns401ForUnknownClientId() {
      StepVerifier.create(controller.getToken(tokenRequest("unknown-client", TEST_CLIENT_SECRET, null)))
          .assertNext(response -> {
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).containsEntry("error", "authentication_failed");
          })
          .verifyComplete();
    }
  }
}
