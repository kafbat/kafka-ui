package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.Role;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rbac")
public class RoleBasedAccessControlProperties {

  private final List<Role> roles = new ArrayList<>();

  @PostConstruct
  public void init() {
    roles.forEach(Role::validate);
  }

  public List<Role> getRoles() {
    return roles;
  }

}
