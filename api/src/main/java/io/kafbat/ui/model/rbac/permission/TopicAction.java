package io.kafbat.ui.model.rbac.permission;

import java.util.Set;
import org.apache.commons.lang3.EnumUtils;
import org.jetbrains.annotations.Nullable;

public enum TopicAction implements PermissibleAction {

  VIEW,
  CREATE(VIEW),
  EDIT(VIEW),
  DELETE(VIEW),
  MESSAGES_READ(VIEW),
  MESSAGES_PRODUCE(VIEW),
  MESSAGES_DELETE(VIEW, EDIT),
  ANALYSIS_VIEW(VIEW),
  ANALYSIS_RUN(VIEW, ANALYSIS_VIEW),

  ;

  public static final Set<TopicAction> ALTER_ACTIONS = Set.of(CREATE, EDIT, DELETE, MESSAGES_PRODUCE, MESSAGES_DELETE);

  private final TopicAction[] dependantActions;

  TopicAction(TopicAction... dependantActions) {
    this.dependantActions = dependantActions;
  }

  @Nullable
  public static TopicAction fromString(String name) {
    return EnumUtils.getEnum(TopicAction.class, name);
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
