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
 * Tests that OAuth2 requests BYPASS the proxy when proxy is NOT configured.
 * Verifies requests arrive ONLY at OAuth server, NOT at proxy.
 */
class OAuthWithoutProxyTest {

  @AfterAll
  static void stopServers() {
    OAuthTestSupport.stopServers();
  }

  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithoutProxyInitializer.class)
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
              .withBody("{\"access_token\":\"direct-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
    }

    @Test
    void tokenRequestBypassesProxy() {
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
          .assertNext(response -> assertThat(response.getAccessToken().getTokenValue()).isEqualTo("direct-token"))
          .verifyComplete();

      OAuthTestSupport.getOAuthServer().verify(
          postRequestedFor(urlPathEqualTo(TOKEN_PATH)).withRequestBody(containing("code=code")));
      assertThat(OAuthTestSupport.getProxyServer().getAllServeEvents()).isEmpty();
    }
  }

  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithoutProxyInitializer.class)
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
              .withBody("{\"sub\":\"direct-user\",\"name\":\"Direct User\"}")));
    }

    @Test
    void userInfoRequestBypassesProxy() {
      var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token",
          Instant.now(), Instant.now().plus(Duration.ofHours(1)));
      var request = new OAuth2UserRequest(OAuthTestSupport.testClientRegistration(), token);

      StepVerifier.create(userService.loadUser(request))
          .assertNext(user -> {
            assertThat(user.getName()).isEqualTo("direct-user");
            assertThat(user.getAttributes()).containsEntry("name", "Direct User");
          })
          .verifyComplete();

      OAuthTestSupport.getOAuthServer().verify(getRequestedFor(urlPathEqualTo(USERINFO_PATH)));
      assertThat(OAuthTestSupport.getProxyServer().getAllServeEvents()).isEmpty();
    }
  }

}
