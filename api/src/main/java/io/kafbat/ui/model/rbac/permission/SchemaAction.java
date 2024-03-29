package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum SchemaAction implements PermissibleAction {

  VIEW,
  CREATE(VIEW),
  DELETE(VIEW),
  EDIT(VIEW),
  MODIFY_GLOBAL_COMPATIBILITY

  ;

  public static final Set<SchemaAction> ALTER_ACTIONS = Set.of(CREATE, DELETE, EDIT, MODIFY_GLOBAL_COMPATIBILITY);

  private final SchemaAction[] dependantActions;

  SchemaAction(SchemaAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  @Nullable
  public static SchemaAction fromString(String name) {
    return EnumUtils.getEnum(SchemaAction.class, name);
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
