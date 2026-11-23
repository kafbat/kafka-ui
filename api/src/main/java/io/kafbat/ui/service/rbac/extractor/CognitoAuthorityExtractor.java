package io.kafbat.ui.service.rbac.extractor;

import static io.kafbat.ui.model.rbac.provider.Provider.Name.COGNITO;

import com.google.common.collect.Sets;
import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Slf4j
public class CognitoAuthorityExtractor implements ProviderAuthorityExtractor {

  public static final String ROLES_FIELD_PARAM_NAME = "roles-field";
  private static final String COGNITO_GROUPS_ATTRIBUTE_NAME = "cognito:groups";

  @Override
  public boolean isApplicable(String provider, Map<String, String> customParams) {
    return COGNITO.equalsIgnoreCase(provider) || COGNITO.equalsIgnoreCase(customParams.get(TYPE));
  }

  @Override
  public Mono<Set<String>> extract(AccessControlService acs, Object value, Map<String, Object> additionalParams) {
    log.debug("Extracting cognito user authorities");

    DefaultOAuth2User principal;
    try {
      principal = (DefaultOAuth2User) value;
    } catch (ClassCastException e) {
      log.error("Can't cast value to DefaultOAuth2User", e);
      throw new RuntimeException();
    }

    var usernameRoles = extractUsernameRoles(acs, principal);
    var groupRoles = extractGroupRoles(acs, principal, additionalParams);

    return Mono.just(Sets.union(usernameRoles, groupRoles));
  }

  private Set<String> extractUsernameRoles(AccessControlService acs, DefaultOAuth2User principal) {
    Set<String> rolesByUsername = acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH_COGNITO))
            .filter(s -> s.getType().equals("user"))
            .anyMatch(s -> s.matches(principal.getName())))
        .map(Role::getName)
        .collect(Collectors.toSet());

    log.debug("Matched user roles: [{}]", String.join(", ", rolesByUsername));

    return rolesByUsername;
  }

  private Set<String> extractGroupRoles(AccessControlService acs, DefaultOAuth2User principal,
                                        Map<String, Object> additionalParams) {
    var provider = (OAuthProperties.OAuth2Provider) additionalParams.get("provider");
    Assert.notNull(provider, "provider is null");

    var rolesFieldName = Optional.ofNullable(provider.getCustomParams().get(ROLES_FIELD_PARAM_NAME))
        .orElse(COGNITO_GROUPS_ATTRIBUTE_NAME);

    List<String> groups = principal.getAttribute(rolesFieldName);
    if (groups == null) {
      log.debug("Cognito groups param is not present");
      return Collections.emptySet();
    }

    log.debug("Token's groups: [{}]", String.join(",", groups));

    Set<String> rolesByGroups = acs.getRoles()
        .stream()
        .filter(role -> role.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH_COGNITO))
            .filter(s -> s.getType().equals("group"))
            .anyMatch(subject -> groups
                .stream()
                .anyMatch(subject::matches)
            ))
        .map(Role::getName)
        .collect(Collectors.toSet());

    log.debug("Matched group roles: [{}]", String.join(", ", rolesByGroups));

    return rolesByGroups;
  }

}
