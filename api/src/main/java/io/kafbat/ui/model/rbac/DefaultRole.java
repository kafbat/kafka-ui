package io.kafbat.ui.model.rbac;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DefaultRole {

  private List<String> clusters;
  private List<Permission> permissions = new ArrayList<>();

  public void validate() {
    checkArgument(clusters != null && !clusters.isEmpty(), "Default role clusters cannot be empty");
    permissions.forEach(Permission::validate);
    permissions.forEach(Permission::transform);
  }
}