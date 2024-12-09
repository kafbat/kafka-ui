package io.kafbat.ui.model.rbac;

import com.google.common.base.Preconditions;
import java.util.List;
import lombok.Data;

@Data
public class Role {

  String name;
  List<String> clusters;
  List<Subject> subjects;
  List<Permission> permissions;

  public void validate() {
    Preconditions.checkArgument(!clusters.isEmpty(), "Role clusters cannot be empty");
    permissions.forEach(Permission::transform);
    permissions.forEach(Permission::validate);
  }

}
