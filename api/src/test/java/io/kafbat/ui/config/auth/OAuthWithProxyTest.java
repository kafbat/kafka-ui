package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.kafbat.ui.config.auth.OAuthTestSupport.TOKEN_PATH;
import static io.kafbat.ui.config.auth.OAuthTestSupport.USERINFO_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.test.StepVerifier;

/**
 * Tests that OAuth2 requests go THROUGH the proxy when proxy is configured.
 * Verifies requests arrive at BOTH proxy and OAuth server.
 */
class OAuthWithProxyTest {

  @AfterAll
  static void stopServers() {
    OAuthTestSupport.stopServers();
  }

  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithProxyInitializer.class)
  @DirtiesContext
  @ActiveProfiles("test")
  class TokenEndpoint {

    @Autowired
    ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient;

    @Autowired
    ReactiveClientRegistrationRepository clientRepo;

    @BeforeEach
    void setup() {
      OAuthTestSupport.resetServers();
      OAuthTestSupport.getOAuthServer().stubFor(post(urlPathEqualTo(TOKEN_PATH))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"access_token\":\"proxy-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
    }

    @Test
    void tokenRequestGoesThruProxy() {
      var registration = clientRepo.findByRegistrationId("test").block();
      assertThat(registration).isNotNull();

      var authRequest = OAuth2AuthorizationRequest.authorizationCode()
          .clientId(registration.getClientId())
          .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
          .redirectUri("http://localhost/callback")
          .scopes(registration.getScopes())
          .state("state")
          .build();
      var authResponse = OAuth2AuthorizationResponse.success("code")
          .redirectUri("http://localhost/callback")
          .state("state")
          .build();
      var grantRequest = new OAuth2AuthorizationCodeGrantRequest(
          registration, new OAuth2AuthorizationExchange(authRequest, authResponse));

      StepVerifier.create(tokenClient.getTokenResponse(grantRequest))
          .assertNext(response -> assertThat(response.getAccessToken().getTokenValue()).isEqualTo("proxy-token"))
          .verifyComplete();

      OAuthTestSupport.getOAuthServer().verify(
          postRequestedFor(urlPathEqualTo(TOKEN_PATH)).withRequestBody(containing("code=code")));
      OAuthTestSupport.getProxyServer().verify(
          postRequestedFor(urlPathEqualTo(TOKEN_PATH)).withRequestBody(containing("code=code")));
    }
  }

  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithProxyInitializer.class)
  @DirtiesContext
  @ActiveProfiles("test")
  class UserInfo {

    @Autowired
    ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService;

    @BeforeEach
    void setup() {
      OAuthTestSupport.resetServers();
      OAuthTestSupport.getOAuthServer().stubFor(get(urlPathEqualTo(USERINFO_PATH))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"sub\":\"user123\",\"name\":\"Test User\"}")));
    }

    @Test
    void userInfoRequestGoesThruProxy() {
      var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
          Instant.now(), Instant.now().plus(Duration.ofHours(1)));
      var request = new OAuth2UserRequest(OAuthTestSupport.testClientRegistration(), token);

      StepVerifier.create(userService.loadUser(request))
          .assertNext(user -> {
            assertThat(user.getName()).isEqualTo("user123");
            assertThat(user.getAttributes()).containsEntry("name", "Test User");
          })
          .verifyComplete();

      OAuthTestSupport.getOAuthServer().verify(getRequestedFor(urlPathEqualTo(USERINFO_PATH)));
      OAuthTestSupport.getProxyServer().verify(getRequestedFor(urlPathEqualTo(USERINFO_PATH)));
    }
  }

  /**
   * Tests non-OIDC OAuth2 flow (GitHub-style) where no "openid" scope is present.
   * This verifies that DelegatingReactiveAuthenticationManager correctly falls back
   * to OAuth2LoginReactiveAuthenticationManager when OidcAuthorizationCodeReactiveAuthenticationManager
   * returns empty (due to missing "openid" scope).
   */
  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithProxyInitializer.class)
  @DirtiesContext
  @ActiveProfiles("test")
  class GitHubStyleOAuth2 {

    @Autowired
    ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService;

    @BeforeEach
    void setup() {
      OAuthTestSupport.resetServers();
      // GitHub-style userinfo response (uses "login" as username attribute)
      OAuthTestSupport.getOAuthServer().stubFor(get(urlPathEqualTo(USERINFO_PATH))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody("{\"login\":\"octocat\",\"id\":1,\"name\":\"The Octocat\"}")));
    }

    @Test
    void nonOidcUserInfoRequestSucceeds() {
      var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "gh-token",
          Instant.now(), Instant.now().plus(Duration.ofHours(1)));
      // Use GitHub-style registration (no "openid" scope)
      var request = new OAuth2UserRequest(OAuthTestSupport.githubStyleClientRegistration(), token);

      StepVerifier.create(userService.loadUser(request))
          .assertNext(user -> {
            // GitHub uses "login" as the username attribute
            assertThat(user.getName()).isEqualTo("octocat");
            assertThat(user.getAttributes()).containsEntry("login", "octocat");
            assertThat(user.getAttributes()).containsEntry("name", "The Octocat");
          })
          .verifyComplete();

      OAuthTestSupport.getOAuthServer().verify(getRequestedFor(urlPathEqualTo(USERINFO_PATH)));
    }
  }

}
