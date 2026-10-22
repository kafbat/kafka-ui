package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.YamlPropertySourceFactory;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(RoleBasedAccessControlProperties.class)
@TestPropertySource(
    locations = "classpath:application-roles-definition.yml",
    factory = YamlPropertySourceFactory.class
)
class RbacReactiveJwtAuthenticationConverterTest {

  @Autowired
  private RoleBasedAccessControlProperties properties;

  private AccessControlService accessControlService;
  private RbacReactiveJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    accessControlService = new AccessControlServiceMock(properties.getRoles()).getMock();
    converter = new RbacReactiveJwtAuthenticationConverter(
        accessControlService, "roles", "username");
  }

  @Test
  void extractsRolesFromListClaim() {
    Jwt jwt = buildJwt(Map.of(
        "username", "someone@example.com",
        "roles", List.of("ROLE-ADMIN", "ANOTHER-ROLE")
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    assertThat(token.getPrincipal()).isInstanceOf(RbacBearerTokenUser.class);
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).contains("admin");
  }

  @Test
  void extractsRolesFromCommaSeparatedString() {
    Jwt jwt = buildJwt(Map.of(
        "username", "someone@example.com",
        "roles", "ROLE-ADMIN, ROLE_EDITOR,"
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).contains("admin", "editor");
  }

  @Test
  void ignoresNonStringRoles() {
    Jwt jwt = buildJwt(Map.of(
        "username", "someone@example.com",
        "roles", Arrays.asList(42, null, "ROLE-ADMIN")
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).containsExactly("admin");
  }

  @Test
  void matchesUsernameSubject() {
    Jwt jwt = buildJwt(Map.of(
        "username", "john@kafka.com",
        "roles", List.of()
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).contains("viewer");
    assertThat(principal.name()).isEqualTo("john@kafka.com");
    assertThat(token.getName()).isEqualTo("john@kafka.com");
  }

  @Test
  void noMatchingRolesReturnsEmptyGroups() {
    Jwt jwt = buildJwt(Map.of(
        "username", "nobody@unknown.com",
        "roles", List.of("UNMATCHED_ROLE")
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).isEmpty();
  }

  @Test
  void missingRolesClaimReturnsEmptyGroups() {
    Jwt jwt = buildJwt(Map.of(
        "username", "nobody@unknown.com"
    ));

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.groups()).isEmpty();
  }

  @Test
  void missingUsernameClaimDoesNotMatchUserSubjectsBySub() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("sub-fallback-user@kafka.com")
        .claim("roles", List.of("ROLE-ADMIN"))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();

    AbstractAuthenticationToken token = converter.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.name()).isEqualTo("sub-fallback-user@kafka.com");
    assertThat(principal.groups()).containsExactly("admin");
  }

  @Test
  void usernameComesFromSubWhenNoUsernameClaimIsConfigured() {
    var converterBySub = new RbacReactiveJwtAuthenticationConverter(accessControlService, "roles", null);
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("john@kafka.com")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();

    AbstractAuthenticationToken token = converterBySub.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.name()).isEqualTo("john@kafka.com");
    assertThat(principal.groups()).containsExactly("viewer");
  }

  @Test
  void nullRolesClaimConfigStillExtractsUsername() {
    var converterNoRoles = new RbacReactiveJwtAuthenticationConverter(
        accessControlService, null, "username");

    Jwt jwt = buildJwt(Map.of(
        "username", "john@kafka.com"
    ));

    AbstractAuthenticationToken token = converterNoRoles.convert(jwt).block();

    assertThat(token).isNotNull();
    RbacBearerTokenUser principal = (RbacBearerTokenUser) token.getPrincipal();
    assertThat(principal.name()).isEqualTo("john@kafka.com");
    assertThat(principal.groups()).contains("viewer");
  }

  private Jwt buildJwt(Map<String, Object> claims) {
    var builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject("default-sub")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300));

    claims.forEach(builder::claim);
    return builder.build();
  }
}
