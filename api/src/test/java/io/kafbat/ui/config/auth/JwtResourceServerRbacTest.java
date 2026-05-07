package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Resource;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

class JwtResourceServerRbacTest {

  private static WireMockServer wireMockServer;
  private static RSAKey rsaKey;

  @BeforeAll
  static void startWireMock() throws Exception {
    rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
    wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMockServer.start();

    wireMockServer.stubFor(get(urlPathEqualTo("/.well-known/jwks.json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody(new JWKSet(rsaKey.toPublicJWK()).toString())));
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  private AccessControlService buildAccessControlService() {
    AccessControlService acs = mock(AccessControlService.class);
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getOauthExtractors()).thenReturn(Collections.<ProviderAuthorityExtractor>emptySet());

    Role streamingRole = buildRole("streaming-role", "streaming", "role");
    Role adminRole = buildRole("admin-role", "admin@company.com", "user");

    when(acs.getRoles()).thenReturn(List.of(streamingRole, adminRole));
    return acs;
  }

  private ReactiveJwtDecoder buildJwtDecoder() {
    return NimbusReactiveJwtDecoder
        .withJwkSetUri("http://localhost:" + wireMockServer.port() + "/.well-known/jwks.json")
        .build();
  }

  @Test
  void validJwtWithMatchingRolesProducesAuthenticatedToken() throws Exception {
    AccessControlService acs = buildAccessControlService();
    var converter = new RbacReactiveJwtAuthenticationConverter(acs, "ter", "tei");
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    String tokenValue = buildSignedJwt("trey@example.com", List.of("streaming"));
    Jwt jwt = decoder.decode(tokenValue).block();

    assertThat(jwt).isNotNull();
    AbstractAuthenticationToken authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    assertThat(authToken.isAuthenticated()).isTrue();
    assertThat(authToken.getPrincipal()).isInstanceOf(RbacJwtUser.class);

    RbacJwtUser principal = (RbacJwtUser) authToken.getPrincipal();
    assertThat(principal.name()).isEqualTo("trey@example.com");
    assertThat(principal.groups()).containsExactly("streaming-role");
  }

  @Test
  void validJwtWithUsernameMatchProducesAuthenticatedToken() throws Exception {
    AccessControlService acs = buildAccessControlService();
    var converter = new RbacReactiveJwtAuthenticationConverter(acs, "ter", "tei");
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    String tokenValue = buildSignedJwt("admin@company.com", List.of());
    Jwt jwt = decoder.decode(tokenValue).block();

    assertThat(jwt).isNotNull();
    AbstractAuthenticationToken authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    RbacJwtUser principal = (RbacJwtUser) authToken.getPrincipal();
    assertThat(principal.name()).isEqualTo("admin@company.com");
    assertThat(principal.groups()).containsExactly("admin-role");
  }

  @Test
  void validJwtWithNoMatchingRolesProducesEmptyGroups() throws Exception {
    AccessControlService acs = buildAccessControlService();
    var converter = new RbacReactiveJwtAuthenticationConverter(acs, "ter", "tei");
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    String tokenValue = buildSignedJwt("nobody@unknown.com", List.of("unmatched"));
    Jwt jwt = decoder.decode(tokenValue).block();

    assertThat(jwt).isNotNull();
    AbstractAuthenticationToken authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    RbacJwtUser principal = (RbacJwtUser) authToken.getPrincipal();
    assertThat(principal.groups()).isEmpty();
  }

  @Test
  void invalidJwtIsRejectedByDecoder() {
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    StepVerifier.create(decoder.decode("invalid.jwt.token"))
        .expectError()
        .verify();
  }

  @Test
  void jwtWithBothRoleAndUsernameMatchProducesUnionOfGroups() throws Exception {
    AccessControlService acs = buildAccessControlService();
    var converter = new RbacReactiveJwtAuthenticationConverter(acs, "ter", "tei");
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    String tokenValue = buildSignedJwt("admin@company.com", List.of("streaming"));
    Jwt jwt = decoder.decode(tokenValue).block();

    assertThat(jwt).isNotNull();
    AbstractAuthenticationToken authToken = converter.convert(jwt).block();

    assertThat(authToken).isNotNull();
    RbacJwtUser principal = (RbacJwtUser) authToken.getPrincipal();
    assertThat(principal.groups()).containsExactlyInAnyOrder("streaming-role", "admin-role");
  }

  @Test
  void principalImplementsRbacUser() throws Exception {
    AccessControlService acs = buildAccessControlService();
    var converter = new RbacReactiveJwtAuthenticationConverter(acs, "ter", "tei");
    ReactiveJwtDecoder decoder = buildJwtDecoder();

    String tokenValue = buildSignedJwt("trey@example.com", List.of("streaming"));
    Jwt jwt = decoder.decode(tokenValue).block();

    AbstractAuthenticationToken authToken = converter.convert(jwt).block();
    assertThat(authToken.getPrincipal()).isInstanceOf(RbacUser.class);

    RbacUser rbacUser = (RbacUser) authToken.getPrincipal();
    assertThat(rbacUser.name()).isEqualTo("trey@example.com");
    assertThat(rbacUser.groups()).contains("streaming-role");
  }

  private String buildSignedJwt(String username, List<String> roles) throws Exception {
    JWSSigner signer = new RSASSASigner(rsaKey);
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .subject(username)
        .claim("tei", username)
        .claim("ter", roles)
        .issueTime(new Date())
        .expirationTime(new Date(System.currentTimeMillis() + 300_000))
        .build();

    SignedJWT signed = new SignedJWT(
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
        claims);
    signed.sign(signer);
    return signed.serialize();
  }

  private static Role buildRole(String roleName, String subjectValue, String subjectType) {
    Role role = new Role();
    role.setName(roleName);
    role.setClusters(List.of("local"));

    Subject subject = new Subject();
    subject.setProvider(Provider.OAUTH);
    subject.setType(subjectType);
    subject.setValue(subjectValue);
    role.setSubjects(List.of(subject));

    Permission permission = new Permission();
    permission.setResource(Resource.APPLICATIONCONFIG.name());
    permission.setActions(List.of("all"));
    role.setPermissions(List.of(permission));
    role.validate();
    return role;
  }
}
