package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.UriComponentsBuilder;

class OAuthPkceTest {

  private static final String AUTHORIZATION_URI = "https://identity.example.test/oauth/authorize";
  private static final String CALLBACK = "https://kafbat.example.test/login/oauth2/code/kafbat";

  private OAuth2AuthorizationRequest authorize() {
    ClientRegistration client = ClientRegistration.withRegistrationId("kafbat")
        .clientId("kafbat")
        .clientSecret("test-secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(CALLBACK)
        .scope("openid", "profile")
        .authorizationUri(AUTHORIZATION_URI)
        .tokenUri("https://identity.example.test/oauth/token")
        .jwkSetUri("https://identity.example.test/oauth/jwks")
        .issuerUri("https://identity.example.test")
        .build();
    var repo = new InMemoryReactiveClientRegistrationRepository(client);
    var resolver = new OAuthSecurityConfig(new OAuthProperties()).authorizationRequestResolver(repo);
    MockServerHttpRequest request = MockServerHttpRequest
        .get("https://kafbat.example.test/oauth2/authorization/kafbat").build();
    return resolver.resolve(MockServerWebExchange.from(request)).block();
  }

  @Test
  void sendsS256ChallengeWithMatchingVerifierForConfidentialClient() throws Exception {
    var request = authorize();
    assertThat(request).isNotNull();
    String verifier = request.getAttribute("code_verifier");
    assertThat(verifier).isNotBlank();
    byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(verifier.getBytes(StandardCharsets.US_ASCII));
    String expectedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    var query = UriComponentsBuilder.fromUriString(request.getAuthorizationRequestUri())
        .build().getQueryParams();
    assertThat(query.getFirst("code_challenge")).isEqualTo(expectedChallenge);
    assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
    assertThat(query.getFirst("client_id")).isEqualTo("kafbat");
    assertThat(query.getFirst("redirect_uri")).isEqualTo(CALLBACK);
    assertThat(request.getState()).isNotBlank();
    assertThat(query.getFirst("nonce")).isNotBlank();
  }

  @Test
  void independentLoginsDoNotShareVerifiersOrStates() {
    var first = authorize();
    var second = authorize();
    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    String firstVerifier = first.getAttribute("code_verifier");
    String secondVerifier = second.getAttribute("code_verifier");
    assertThat(firstVerifier).isNotEqualTo(secondVerifier);
    assertThat(first.getState()).isNotEqualTo(second.getState());
  }
}
