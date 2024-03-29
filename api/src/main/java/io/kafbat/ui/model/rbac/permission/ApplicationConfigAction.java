package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum ApplicationConfigAction implements PermissibleAction {

  VIEW,
  EDIT(VIEW)

  ;

  public static final Set<ApplicationConfigAction> ALTER_ACTIONS = Set.of(EDIT);

  private final PermissibleAction[] dependantActions;

  ApplicationConfigAction(ApplicationConfigAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  @Nullable
  public static ApplicationConfigAction fromString(String name) {
    return EnumUtils.getEnum(ApplicationConfigAction.class, name);
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
