package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.Role;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rbac")
public class RoleBasedAccessControlProperties {

  private final List<Role> roles = new ArrayList<>();

  private Role defaultRole;

  @PostConstruct
  public void init() {
    roles.forEach(Role::validate);
    if (defaultRole != null) {
      defaultRole.validateDefaultRole();
    }
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setDefaultRole(Role defaultRole) {
    this.defaultRole = defaultRole;
  }

  @Nullable
  public Role getDefaultRole() {
    return defaultRole;
  }
}
