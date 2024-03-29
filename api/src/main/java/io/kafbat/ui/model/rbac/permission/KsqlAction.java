package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum KsqlAction implements PermissibleAction {

  EXECUTE

  ;

  public static final Set<KsqlAction> ALTER_ACTIONS = Set.of(EXECUTE);

  private final KsqlAction[] dependantActions;

  KsqlAction(KsqlAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  @Nullable
  public static KsqlAction fromString(String name) {
    return EnumUtils.getEnum(KsqlAction.class, name);
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
