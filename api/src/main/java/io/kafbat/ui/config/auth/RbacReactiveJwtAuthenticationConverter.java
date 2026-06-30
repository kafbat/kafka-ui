package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@Slf4j
public class RbacReactiveJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final AccessControlService accessControlService;
  private final String rolesClaim;
  private final String usernameClaim;
  private final String entityTypeClaim;
  private final String defaultRole;

  public RbacReactiveJwtAuthenticationConverter(
      AccessControlService accessControlService,
      String rolesClaim,
      String usernameClaim,
      String entityTypeClaim,
      String defaultRole) {
    this.accessControlService = accessControlService;
    this.rolesClaim = rolesClaim;
    this.usernameClaim = usernameClaim;
    this.entityTypeClaim = entityTypeClaim;
    this.defaultRole = defaultRole;
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    String username = extractUsername(jwt);
    Collection<String> tokenRoles = extractTokenRoles(jwt);
    String entityType = extractEntityType(jwt);

    log.debug("JWT principal: [{}], token roles: [{}], entity type: [{}]",
        username, tokenRoles, entityType);

    Set<String> matchedGroups = matchRbacRoles(username, tokenRoles, entityType);

    if (matchedGroups.isEmpty() && defaultRole != null) {
      log.debug("No RBAC roles matched, applying default role: [{}]", defaultRole);
      matchedGroups = Set.of(defaultRole);
    }

    log.debug("Matched RBAC groups: [{}]", matchedGroups);

    RbacJwtUser rbacUser = new RbacJwtUser(jwt, username, matchedGroups);
    var authorities = matchedGroups.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    return Mono.just(new RbacJwtAuthenticationToken(rbacUser, jwt, authorities));
  }

  private String extractUsername(Jwt jwt) {
    if (usernameClaim != null) {
      Object claim = jwt.getClaim(usernameClaim);
      if (claim != null) {
        return claim.toString();
      }
    }
    return jwt.getSubject();
  }

  @SuppressWarnings("unchecked")
  private Collection<String> extractTokenRoles(Jwt jwt) {
    if (rolesClaim == null) {
      return Collections.emptyList();
    }
    Object claim = jwt.getClaim(rolesClaim);
    if (claim == null) {
      return Collections.emptyList();
    }
    if (claim instanceof Collection<?>) {
      return ((Collection<?>) claim).stream()
          .map(Object::toString)
          .collect(Collectors.toList());
    }
    if (claim instanceof String str) {
      return Arrays.asList(str.split(","));
    }
    return Collections.emptyList();
  }

  private String extractEntityType(Jwt jwt) {
    if (entityTypeClaim == null) {
      return null;
    }
    Object claim = jwt.getClaim(entityTypeClaim);
    return claim != null ? claim.toString() : null;
  }

  private Set<String> matchRbacRoles(String username, Collection<String> tokenRoles,
                                     String entityType) {
    List<Role> roles = accessControlService.getRoles();

    Set<String> usernameMatches = roles.stream()
        .filter(r -> r.getSubjects().stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> "user".equals(s.getType()))
            .anyMatch(s -> s.matches(username)))
        .map(Role::getName)
        .collect(Collectors.toSet());

    Set<String> roleMatches = roles.stream()
        .filter(role -> role.getSubjects().stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> "role".equals(s.getType()))
            .anyMatch(subject -> tokenRoles.stream().anyMatch(subject::matches)))
        .map(Role::getName)
        .collect(Collectors.toSet());

    Set<String> entityTypeMatches = Collections.emptySet();
    if (entityType != null) {
      entityTypeMatches = roles.stream()
          .filter(role -> role.getSubjects().stream()
              .filter(s -> s.getProvider().equals(Provider.OAUTH))
              .filter(s -> "entity-type".equals(s.getType()))
              .anyMatch(s -> s.matches(entityType)))
          .map(Role::getName)
          .collect(Collectors.toSet());
    }

    Set<String> combined = new HashSet<>(usernameMatches);
    combined.addAll(roleMatches);
    combined.addAll(entityTypeMatches);
    return combined;
  }
}
