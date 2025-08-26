package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.Role;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rbac")
public class RoleBasedAccessControlProperties {

  private List<Role> roles = new ArrayList<>();
//  private String haha;

  @PostConstruct
  public void init() {
    roles.forEach(Role::validate);
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
    init();
  }

}
