package io.kafbat.ui.model.rbac.permission;

public sealed interface PermissibleAction permits
    AclAction, ApplicationConfigAction,
    ConsumerGroupAction, SchemaAction,
    ConnectAction, ClusterConfigAction,
    KsqlAction, TopicAction, AuditAction, ClientQuotaAction {

  String name();

  boolean isAlter();

}
