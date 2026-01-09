package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.kafbat.ui.config.auth.OAuthTestSupport.JWKS_PATH;
import static io.kafbat.ui.config.auth.OAuthTestSupport.TOKEN_PATH;
import static io.kafbat.ui.config.auth.OAuthTestSupport.USERINFO_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
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

  @Nested
  @SpringBootTest(
      classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
      properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
  @ContextConfiguration(initializers = OAuthTestSupport.WithProxyInitializer.class)
  @DirtiesContext
  @ActiveProfiles("test")
  class JwksEndpoint {

    @Autowired
    @Qualifier("oauthWebClient")
    WebClient oauthWebClient;

    @BeforeEach
    void setup() {
      OAuthTestSupport.resetServers();
      // Stub JWKS endpoint with the test RSA key
      String jwksJson = "{\"keys\":[" + OAuthTestSupport.getRsaKey().toPublicJWK().toJSONString() + "]}";
      OAuthTestSupport.getOAuthServer().stubFor(get(urlPathEqualTo(JWKS_PATH))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
              .withBody(jwksJson)));
    }

    @Test
    void jwksRequestGoesThruProxy() throws Exception {
      // Create a JWT decoder using oauthWebClient - same as setJwtDecoderFactory does
      var jwtDecoder = NimbusReactiveJwtDecoder
          .withJwkSetUri(OAuthTestSupport.oauthBaseUrl() + JWKS_PATH)
          .webClient(oauthWebClient)
          .build();

      // Create a valid JWT signed with our test key
      var claims = new JWTClaimsSet.Builder()
          .subject("test-user")
          .issuer(OAuthTestSupport.oauthBaseUrl())
          .expirationTime(new Date(System.currentTimeMillis() + 300000))
          .issueTime(new Date())
          .build();
      var signedJwt = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(OAuthTestSupport.getRsaKey().getKeyID()).build(),
          claims);
      signedJwt.sign(new RSASSASigner(OAuthTestSupport.getRsaKey()));
      String token = signedJwt.serialize();

      // Decode the JWT - this triggers JWKS fetch
      StepVerifier.create(jwtDecoder.decode(token))
          .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo("test-user"))
          .verifyComplete();

      // Verify JWKS request went through both OAuth server and proxy
      OAuthTestSupport.getOAuthServer().verify(getRequestedFor(urlPathEqualTo(JWKS_PATH)));
      OAuthTestSupport.getProxyServer().verify(getRequestedFor(urlPathEqualTo(JWKS_PATH)));
    }
  }

}
