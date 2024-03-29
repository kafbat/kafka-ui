package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum ClusterConfigAction implements PermissibleAction {

  VIEW,
  EDIT(VIEW)

  ;

  public static final Set<ClusterConfigAction> ALTER_ACTIONS = Set.of(EDIT);

  private final ClusterConfigAction[] dependantActions;

  ClusterConfigAction(ClusterConfigAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  @Nullable
  public static ClusterConfigAction fromString(String name) {
    return EnumUtils.getEnum(ClusterConfigAction.class, name);
  }

  @Override
  public boolean isAlter() {
    return ALTER_ACTIONS.contains(this);
  }

  @Override
  public PermissibleAction[] dependantActions() {
    return dependantActions;
  }
}
