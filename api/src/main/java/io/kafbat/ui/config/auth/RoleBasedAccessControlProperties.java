package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.rbac.Role;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties("rbac")
public class RoleBasedAccessControlProperties {

  private volatile List<Role> roles = new ArrayList<>();

  @PostConstruct
  public void init() {
    roles.forEach(Role::validate);
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
    init();
  }

}
