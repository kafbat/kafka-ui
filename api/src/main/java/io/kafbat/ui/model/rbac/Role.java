package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Role {

  String name;
  List<String> clusters = new ArrayList<>();
  List<Subject> subjects = new ArrayList<>();
  List<Permission> permissions = new ArrayList<>();

  public void validate() {
    checkArgument(!clusters.isEmpty(), "Role clusters cannot be empty");
    checkArgument(!subjects.isEmpty(), "Role subjects cannot be empty");
    permissions.forEach(Permission::validate);
    permissions.forEach(Permission::transform);
    subjects.forEach(Subject::validate);
  }
}
