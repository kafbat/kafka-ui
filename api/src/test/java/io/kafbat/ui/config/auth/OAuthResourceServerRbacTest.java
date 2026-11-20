package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "auth.oauth2.resource-server.jwt.issuer-uri=" + OAuthResourceServerRbacTest.ISSUER,
    "auth.oauth2.resource-server.jwt.audiences[0]=" + OAuthResourceServerRbacTest.AUDIENCE,
    "auth.oauth2.resource-server.jwt.authorities-claim-name=roles",
    "auth.oauth2.resource-server.jwt.authority-prefix=",
    "rbac.roles[0].subjects[0].provider=oauth",
    "rbac.roles[0].subjects[0].type=role",
    "rbac.roles[0].subjects[0].value=kafka-ui-config"
})
class OAuthResourceServerRbacTest extends AbstractBearerTokenRbacTest {

  static final String ISSUER = "https://idp.example.com";
  static final String AUDIENCE = "kafbat-ui";

  private static final RSAKey KEY = generateKey();
  private static final WireMockServer JWKS = startJwks();

  @DynamicPropertySource
  static void jwkSetUri(DynamicPropertyRegistry registry) {
    registry.add("auth.oauth2.resource-server.jwt.jwk-set-uri", () -> JWKS.baseUrl() + "/jwks");
  }

  @AfterAll
  static void stopJwks() {
    JWKS.stop();
  }

  @Test
  void tokenWithMatchingRoleIsAllowed() throws JOSEException {
    validateConfig(token(List.of("kafka-ui-config"), ISSUER, AUDIENCE)).expectStatus().isOk();
  }

  @Test
  void tokenWithoutMatchingRoleIsForbidden() throws JOSEException {
    validateConfig(token(List.of("other"), ISSUER, AUDIENCE)).expectStatus().isForbidden();
  }

  @Test
  void tokenFromAnotherIssuerIsRejected() throws JOSEException {
    validateConfig(token(List.of("kafka-ui-config"), "https://other.example.com", AUDIENCE))
        .expectStatus().isUnauthorized();
  }

  @Test
  void tokenForAnotherAudienceIsRejected() throws JOSEException {
    validateConfig(token(List.of("kafka-ui-config"), ISSUER, "other"))
        .expectStatus().isUnauthorized();
  }

  private static String token(List<String> roles, String issuer, String audience) throws JOSEException {
    var claims = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .audience(audience)
        .subject("service-account")
        .claim("roles", roles)
        .expirationTime(Date.from(Instant.now().plusSeconds(300)))
        .build();
    var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY.getKeyID()).build(), claims);
    jwt.sign(new RSASSASigner(KEY));
    return jwt.serialize();
  }

  private static RSAKey generateKey() {
    try {
      return new RSAKeyGenerator(2048).keyID("test").generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static WireMockServer startJwks() {
    var server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    server.stubFor(get("/jwks").willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(new JWKSet(KEY.toPublicJWK()).toString())));
    return server;
  }
}
