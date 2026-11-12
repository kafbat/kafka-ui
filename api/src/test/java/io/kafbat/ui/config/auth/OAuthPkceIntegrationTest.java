package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.kafbat.ui.config.auth.OAuthTestSupport.TOKEN_PATH;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.test.StepVerifier;

/**
 * Verifies that a confidential OAuth2 client uses the same PKCE verifier during
 * authorization and token exchange against a mock identity provider.
 */
@SpringBootTest(
    classes = {OAuthSecurityConfig.class, OAuthTestSupport.BaseTestConfig.class},
    properties = {"spring.main.allow-bean-definition-overriding=true", "auth.type=OAUTH2"})
@ContextConfiguration(initializers = OAuthTestSupport.WithoutProxyInitializer.class)
@DirtiesContext
@ActiveProfiles("test")
class OAuthPkceIntegrationTest {

  @Autowired
  ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver;

  @Autowired
  ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient;

  @Autowired
  ReactiveClientRegistrationRepository registrations;

  @BeforeEach
  void setup() {
    OAuthTestSupport.resetServers();
    OAuthTestSupport.getOAuthServer().stubFor(post(urlPathEqualTo(TOKEN_PATH))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"access_token\":\"pkce-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));
  }

  @AfterAll
  static void stopServers() {
    OAuthTestSupport.stopServers();
  }

  @Test
  void confidentialClientSendsMatchingVerifierToMockIdentityProvider() throws Exception {
    var registration = registrations.findByRegistrationId(OAuthTestSupport.REGISTRATION_ID).block();
    assertThat(registration).isNotNull();
    assertThat(registration.getClientSecret()).isNotBlank();

    var exchange = MockServerWebExchange.from(MockServerHttpRequest
        .get("http://localhost/oauth2/authorization/" + OAuthTestSupport.REGISTRATION_ID).build());
    var authorization = authorizationRequestResolver.resolve(exchange).block();
    assertThat(authorization).isNotNull();

    String verifier = authorization.getAttribute("code_verifier");
    assertThat(verifier).isNotBlank();
    String expectedChallenge = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256")
            .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    var parameters = UriComponentsBuilder.fromUriString(authorization.getAuthorizationRequestUri())
        .build().getQueryParams();
    assertThat(parameters.getFirst("code_challenge")).isEqualTo(expectedChallenge);
    assertThat(parameters.getFirst("code_challenge_method")).isEqualTo("S256");

    var response = OAuth2AuthorizationResponse.success("mock-auth-code")
        .redirectUri(authorization.getRedirectUri())
        .state(authorization.getState())
        .build();
    var grant = new OAuth2AuthorizationCodeGrantRequest(
        registration, new OAuth2AuthorizationExchange(authorization, response));

    StepVerifier.create(tokenClient.getTokenResponse(grant))
        .assertNext(token -> assertThat(token.getAccessToken().getTokenValue()).isEqualTo("pkce-token"))
        .verifyComplete();

    OAuthTestSupport.getOAuthServer().verify(postRequestedFor(urlPathEqualTo(TOKEN_PATH))
        .withRequestBody(containing("code_verifier=" + verifier))
        .withRequestBody(containing("code=mock-auth-code")));
  }
}
