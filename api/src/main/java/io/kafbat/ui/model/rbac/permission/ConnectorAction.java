package io.kafbat.ui.model.rbac.permission;

import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum ConnectorAction implements PermissibleAction {

  VIEW,
  EDIT(VIEW),
  CREATE(VIEW),
  OPERATE(VIEW),
  DELETE(VIEW),
  RESET_OFFSETS(VIEW),
  ;

  public static final String CONNECTOR_RESOURCE_DELIMITER = "/";

  private final ConnectorAction[] dependantActions;

  ConnectorAction(ConnectorAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  public static final Set<ConnectorAction> ALTER_ACTIONS = Set.of(CREATE, EDIT, DELETE, OPERATE, RESET_OFFSETS);

  public static final Map<String, PermissibleAction> ALIASES = Map.of(
      "restart", OPERATE,
      "pause", OPERATE,
      "resume", OPERATE,
      "restart_task", OPERATE,
      "state_update", OPERATE
  );

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