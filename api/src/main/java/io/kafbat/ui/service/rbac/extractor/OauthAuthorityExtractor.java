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
    var typeMatch = OAUTH.equalsIgnoreCase(provider) || OAUTH.equalsIgnoreCase(customParams.get(TYPE));

    if (!typeMatch) {
      return false;
    }

    var containsRolesFieldNameParam = customParams.containsKey(ROLES_FIELD_PARAM_NAME);
    if (!containsRolesFieldNameParam) {
      log.debug("Provider [{}] doesn't contain a roles field param name, mapping won't be performed", provider);
      return false;
    }

    return true;
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
    return acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> s.getType().equals("user"))
            .anyMatch(s -> s.getValue().equals(principal.getName())))
        .map(Role::getName)
        .collect(Collectors.toSet());
  }

  private Set<String> extractRoles(AccessControlService acs, DefaultOAuth2User principal,
                                   Map<String, Object> additionalParams) {
    var provider = (OAuthProperties.OAuth2Provider) additionalParams.get("provider");
    Assert.notNull(provider, "provider is null");
    var rolesFieldName = provider.getCustomParams().get(ROLES_FIELD_PARAM_NAME);

    var principalRoles = convertRoles(principal.getAttribute(rolesFieldName));
    log.debug("Token's groups: [{}]", String.join(",", principalRoles));

    Set<String> roles = acs.getRoles()
        .stream()
        .filter(role -> role.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH))
            .filter(s -> s.getType().equals("role"))
            .anyMatch(subject -> {
              var roleName = subject.getValue();
              return principalRoles.contains(roleName);
            })
        )
        .map(Role::getName)
        .collect(Collectors.toSet());

    log.debug("Matched roles: [{}]", String.join(", ", roles));

    return roles;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> convertRoles(Object roles) {
    if (roles == null) {
      log.debug("Param missing from attributes, skipping");
      return Collections.emptySet();
    }

    if ((roles instanceof List<?>) || (roles instanceof Set<?>)) {
      log.trace("The field is either a set or a list, returning as is");
      return (Collection<String>) roles;
    }

    if (!(roles instanceof String)) {
      log.debug("The field is not a string, skipping");
      return Collections.emptySet();
    }

    log.trace("Trying to deserialize the field value [{}] as a string", roles);

    return Arrays.stream(((String) roles).split(","))
        .collect(Collectors.toSet());
  }

}
