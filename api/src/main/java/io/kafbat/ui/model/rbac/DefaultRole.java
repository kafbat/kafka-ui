package io.kafbat.ui.model.rbac;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DefaultRole {

  private List<Permission> permissions = new ArrayList<>();

  public void validate() {
    permissions.forEach(Permission::validate);
    permissions.forEach(Permission::transform);
  }
}
