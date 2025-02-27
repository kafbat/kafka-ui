package io.kafbat.ui.service.rbac.extractor;

import static io.kafbat.ui.model.rbac.provider.Provider.Name.GOOGLE;

import com.google.common.collect.Sets;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import reactor.core.publisher.Mono;

@Slf4j
public class GoogleAuthorityExtractor implements ProviderAuthorityExtractor {

  private static final String GOOGLE_DOMAIN_ATTRIBUTE_NAME = "hd";
  public static final String EMAIL_ATTRIBUTE_NAME = "email";

  @Override
  public boolean isApplicable(String provider, Map<String, String> customParams) {
    return GOOGLE.equalsIgnoreCase(provider) || GOOGLE.equalsIgnoreCase(customParams.get(TYPE));
  }

  @Override
  public Mono<Set<String>> extract(AccessControlService acs, Object value, Map<String, Object> additionalParams) {
    log.debug("Extracting google user authorities");

    DefaultOAuth2User principal;
    try {
      principal = (DefaultOAuth2User) value;
    } catch (ClassCastException e) {
      log.error("Can't cast value to DefaultOAuth2User", e);
      throw new RuntimeException();
    }

    var usernameRoles = extractUsernameRoles(acs, principal);
    var domainRoles = extractDomainRoles(acs, principal);

    return Mono.just(Sets.union(usernameRoles, domainRoles));
  }

  private Set<String> extractUsernameRoles(AccessControlService acs, DefaultOAuth2User principal) {
    return acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH_GOOGLE))
            .filter(s -> s.getType().equals("user"))
            .anyMatch(s -> {
              String email = principal.getAttribute(EMAIL_ATTRIBUTE_NAME);
              return s.matches(email);
            }))
        .map(Role::getName)
        .collect(Collectors.toSet());
  }

  private Set<String> extractDomainRoles(AccessControlService acs, DefaultOAuth2User principal) {
    String domain = principal.getAttribute(GOOGLE_DOMAIN_ATTRIBUTE_NAME);
    if (domain == null) {
      log.debug("Google domain param is not present");
      return Collections.emptySet();
    }

    return acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(s -> s.getProvider().equals(Provider.OAUTH_GOOGLE))
            .filter(s -> s.getType().equals("domain"))
            .anyMatch(s -> s.matches(domain)))
        .map(Role::getName)
        .collect(Collectors.toSet());
  }

}
