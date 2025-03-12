package io.kafbat.ui.service.rbac.extractor;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.stream.Collectors;

public class RbacBasicAuthAuthoritiesExtractor {
  private final AccessControlService accessControlService;

  public RbacBasicAuthAuthoritiesExtractor(AccessControlService accessControlService) {
    this.accessControlService = accessControlService;
  }

  public Collection<String> groups(String username) {
    return accessControlService.getRoles().stream()
        .filter(role -> role.getSubjects().stream()
            .filter(subj -> Provider.LOGIN_FORM.equals(subj.getProvider()))
            .filter(subj -> "user".equals(subj.getType()))
            .anyMatch(subj -> username.equals(subj.getValue()))
        )
        .map(Role::getName)
        .collect(Collectors.toSet());
  }
}
