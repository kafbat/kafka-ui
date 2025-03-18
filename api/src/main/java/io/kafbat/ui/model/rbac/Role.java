package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import lombok.Data;

@Data
public class Role {

  String name;
  List<String> clusters;
  List<Subject> subjects;
  List<Permission> permissions;

  public void validate() {
    checkArgument(clusters != null && !clusters.isEmpty(), "Role clusters cannot be empty");
    checkArgument(subjects != null && !subjects.isEmpty(), "Role subjects cannot be empty");
    permissions.forEach(Permission::validate);
    permissions.forEach(Permission::transform);
    subjects.forEach(Subject::validate);
  }

}
