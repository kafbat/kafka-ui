package io.kafbat.ui.service.rbac.extractor;

import static io.kafbat.ui.model.rbac.provider.Provider.Name.OAUTH;

import com.google.common.collect.Sets;
import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Slf4j
public class OauthAuthorityExtractor implements ProviderAuthorityExtractor {

  public static final String ROLES_FIELD_PARAM_NAME = "roles-field";

  @Override
  public boolean isApplicable(String provider, Map<String, String> customParams) {
    return OAUTH.equalsIgnoreCase(provider) || OAUTH.equalsIgnoreCase(customParams.get(TYPE));
  }

  @Override
  public Mono<Set<String>> extract(AccessControlService acs, Object value, Map<String, Object> additionalParams) {
    log.trace("Extracting OAuth2 user authorities");

    DefaultOAuth2User principal;
    try {
      principal = (DefaultOAuth2User) value;
    } catch (ClassCastException e) {
      log.error("Can't cast value to DefaultOAuth2User", e);
      throw new RuntimeException();
    }

    var usernameRoles = extractUsernameRoles(acs, principal);
    var roles = extractRoles(acs, principal, additionalParams);

    return Mono.just(Sets.union(usernameRoles, roles));
  }

  private Set<String> extractUsernameRoles(AccessControlService acs, DefaultOAuth2User principal) {
    var principalName = principal.getName();

    log.debug("Principal name is: [{}]", principalName);

    var roles = acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> s.getType().equals("user"))
            .peek(s -> log.trace("[{}] matches [{}]? [{}]", s.getValue(), principalName,  s.matches(principalName)))
            .anyMatch(s ->  s.matches(principalName)))
        .map(Role::getName)
        .collect(Collectors.toSet());

    log.debug("Matched roles by username: [{}]", String.join(", ", roles));

    return roles;
  }

  private Set<String> extractRoles(AccessControlService acs, DefaultOAuth2User principal,
                                   Map<String, Object> additionalParams) {
    var provider = (OAuthProperties.OAuth2Provider) additionalParams.get("provider");
    Assert.notNull(provider, "provider is null");
    var rolesFieldName = provider.getCustomParams().get(ROLES_FIELD_PARAM_NAME);

    if (rolesFieldName == null) {
      log.warn("Provider [{}] doesn't contain a roles field param name, won't map roles", provider.getClientName());
      return Collections.emptySet();
    }

    var principalRoles = convertRoles(principal.getAttribute(rolesFieldName));
    if (principalRoles.isEmpty()) {
      log.debug("Principal [{}] doesn't have any roles, nothing to do", principal.getName());
      return Collections.emptySet();
    }

    log.debug("Token's groups: [{}]", String.join(",", principalRoles));

    Set<String> roles = acs.getRoles()
        .stream()
        .filter(role -> role.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> s.getType().equals("role"))
            .anyMatch(subject -> principalRoles.stream().anyMatch(subject::matches)))
        .map(Role::getName)
        .collect(Collectors.toSet());

    log.debug("Matched group roles: [{}]", String.join(", ", roles));

    return roles;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> convertRoles(Object roles) {
    if (roles == null) {
      log.warn("Param missing in attributes, nothing to do");
      return Collections.emptySet();
    }

    if ((roles instanceof List<?>) || (roles instanceof Set<?>)) {
      log.trace("The field is either a set or a list, returning as is");
      return (Collection<String>) roles;
    }

    if (!(roles instanceof String)) {
      log.trace("The field is not a string, skipping");
      return Collections.emptySet();
    }

    log.trace("Trying to deserialize the field value [{}] as a string", roles);

    return Arrays.stream(((String) roles).split(","))
        .collect(Collectors.toSet());
  }

}
