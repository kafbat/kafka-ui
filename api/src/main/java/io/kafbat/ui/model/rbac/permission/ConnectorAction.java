package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum ConnectorAction implements PermissibleAction {

  VIEW(ConnectAction.VIEW),
  EDIT(ConnectAction.VIEW, VIEW),
  CREATE(ConnectAction.CREATE, VIEW),
  OPERATE(ConnectAction.OPERATE, VIEW),
  DELETE(ConnectAction.DELETE, VIEW),
  RESET_OFFSETS(ConnectAction.RESET_OFFSETS, VIEW),
  ;

  public static final String CONNECTOR_RESOURCE_DELIMITER = "/";

  private final ConnectAction connectAction;
  private final ConnectorAction[] dependantActions;


  ConnectorAction(ConnectAction connectAction, ConnectorAction... dependantActions) {
    this.connectAction = connectAction;
    this.dependantActions = dependantActions;
  }

  public static final Set<ConnectorAction> ALTER_ACTIONS = Set.of(CREATE, EDIT, DELETE, OPERATE, RESET_OFFSETS);

  public ConnectAction getConnectAction() {
    return connectAction;
  }

  @Nullable
  public static ConnectorAction fromString(String name) {
    return EnumUtils.getEnum(ConnectorAction.class, name);
  }

  @Override
  public boolean isAlter() {
    return ALTER_ACTIONS.contains(this);
  }

  @Override
  public PermissibleAction[] dependantActions() {
    return dependantActions;
  }

  public static String buildResourcePath(String connectName, String connectorName) {
    return connectName + CONNECTOR_RESOURCE_DELIMITER + connectorName;
  }
}
