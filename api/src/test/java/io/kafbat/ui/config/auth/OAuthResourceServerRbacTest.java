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
import io.kafbat.ui.model.ApplicationConfigDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "auth.type=OAUTH2",
    "auth.oauth2.resource-server-rbac.roles-claim=roles",
    "rbac.roles[0].name=config-viewer",
    "rbac.roles[0].clusters[0]=local",
    "rbac.roles[0].subjects[0].provider=oauth",
    "rbac.roles[0].subjects[0].type=role",
    "rbac.roles[0].subjects[0].value=kafka-ui-config",
    "rbac.roles[0].permissions[0].resource=applicationconfig",
    "rbac.roles[0].permissions[0].actions[0]=edit"
})
class OAuthResourceServerRbacTest {

  private static final RSAKey KEY = generateKey();
  private static final WireMockServer JWKS = startJwks();

  @Autowired
  private WebTestClient webTestClient;

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
    validateConfig(List.of("kafka-ui-config")).expectStatus().isOk();
  }

  @Test
  void tokenWithoutMatchingRoleIsForbidden() throws JOSEException {
    validateConfig(List.of("other")).expectStatus().isForbidden();
  }

  private WebTestClient.ResponseSpec validateConfig(List<String> roles) throws JOSEException {
    return webTestClient.put().uri("/api/config/validated")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(roles))
        .bodyValue(new ApplicationConfigDTO().properties(new ApplicationConfigPropertiesDTO()))
        .exchange();
  }

  private static String token(List<String> roles) throws JOSEException {
    var claims = new JWTClaimsSet.Builder()
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
