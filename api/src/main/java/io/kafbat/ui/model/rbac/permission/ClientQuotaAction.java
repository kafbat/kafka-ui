package io.kafbat.ui.model.rbac.permission;

import java.util.Set;

public enum ClientQuotaAction implements PermissibleAction {

  VIEW,
  EDIT(VIEW)

  ;

  public static final Set<ClientQuotaAction> ALTER_ACTIONS = Set.of(EDIT);

  private final PermissibleAction[] dependantActions;

  ClientQuotaAction(ClientQuotaAction... dependantActions) {
    this.dependantActions = dependantActions;
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
