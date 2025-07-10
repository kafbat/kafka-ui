package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Role;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rbac")
public class RoleBasedAccessControlProperties {

  private final List<Role> roles = new ArrayList<>();

  private DefaultRole defaultRole;

  @PostConstruct
  public void init() {
    roles.forEach(Role::validate);
    if (defaultRole != null) {
      defaultRole.validate();
    }
  }

  public List<Role> getRoles() {
    return roles;
  }

  @Nullable
  public DefaultRole getDefaultRole() {
    return defaultRole;
  }
}
