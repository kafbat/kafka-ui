package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.YamlPropertySourceFactory;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

  @BeforeEach
  void setUp() {
    accessControlService = new AccessControlServiceMock(properties.getRoles()).getMock();
  }

  @Test
  void matchesRoleSubjectsFromAuthoritiesClaim() {
    var user = convert(rolesClaim(), jwt("default-sub", Map.of("roles", List.of("ROLE-ADMIN", "OTHER"))));

    assertThat(user.groups()).containsExactly("admin");
  }

  @Test
  void splitsAuthoritiesClaimByConfiguredDelimiter() {
    var user = convert(rolesClaim().andThen(jwt -> jwt.setAuthoritiesClaimDelimiter(",")),
        jwt("default-sub", Map.of("roles", "ROLE-ADMIN,ROLE_EDITOR")));

    assertThat(user.groups()).containsExactlyInAnyOrder("admin", "editor");
  }

  @Test
  void appliesAuthorityPrefix() {
    var user = convert(jwt -> jwt.setAuthoritiesClaimName("roles"),
        jwt("default-sub", Map.of("roles", List.of("ROLE-ADMIN"))));

    assertThat(user.groups()).isEmpty();
  }

  @Test
  void matchesUserSubjectsByPrincipalClaim() {
    var user = convert(jwt -> jwt.setPrincipalClaimName("username"),
        jwt("default-sub", Map.of("username", "john@kafka.com")));

    assertThat(user.name()).isEqualTo("john@kafka.com");
    assertThat(user.groups()).containsExactly("viewer");
  }

  @Test
  void missingPrincipalClaimDoesNotMatchUserSubjectsBySub() {
    var user = convert(jwt -> jwt.setPrincipalClaimName("username"), jwt("john@kafka.com", Map.of()));

    assertThat(user.name()).isEqualTo("john@kafka.com");
    assertThat(user.groups()).isEmpty();
  }

  @Test
  void subIsThePrincipalByDefault() {
    var user = convert(jwt -> { }, jwt("john@kafka.com", Map.of()));

    assertThat(user.name()).isEqualTo("john@kafka.com");
    assertThat(user.groups()).containsExactly("viewer");
  }

  @Test
  void noMatchingSubjectsGiveNoGroups() {
    var user = convert(rolesClaim(), jwt("nobody@unknown.com", Map.of("roles", List.of("UNMATCHED"))));

    assertThat(user.groups()).isEmpty();
  }

  private RbacBearerTokenUser convert(Consumer<OAuth2ResourceServerProperties.Jwt> settings, Jwt jwt) {
    var properties = new OAuth2ResourceServerProperties.Jwt();
    settings.accept(properties);
    var token = new RbacReactiveJwtAuthenticationConverter(accessControlService, properties).convert(jwt).block();
    assertThat(token).isNotNull();
    return (RbacBearerTokenUser) token.getPrincipal();
  }

  private static Consumer<OAuth2ResourceServerProperties.Jwt> rolesClaim() {
    return jwt -> {
      jwt.setAuthoritiesClaimName("roles");
      jwt.setAuthorityPrefix("");
    };
  }

  private static Jwt jwt(String subject, Map<String, Object> claims) {
    var builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300));
    claims.forEach(builder::claim);
    return builder.build();
  }
}
