package io.kafbat.ui.model.rbac;

import io.kafbat.ui.model.rbac.permission.AclAction;
import io.kafbat.ui.model.rbac.permission.ApplicationConfigAction;
import io.kafbat.ui.model.rbac.permission.ClusterConfigAction;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConsumerGroupAction;
import io.kafbat.ui.model.rbac.permission.KsqlAction;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;

public enum Resource {

  APPLICATIONCONFIG(ApplicationConfigAction.values()),

  CLUSTERCONFIG(ClusterConfigAction.values()),

  TOPIC(TopicAction.values()),

  CONSUMER(ConsumerGroupAction.values()),

  SCHEMA(SchemaAction.values()),

  CONNECT(ConnectAction.values()),

  KSQL(KsqlAction.values()),

  ACL(AclAction.values()),

  AUDIT(AclAction.values());

  private final List<PermissibleAction> actions;

  Resource(PermissibleAction[] actions) {
    this.actions = List.of(actions);
  }

  public List<PermissibleAction> allActions() {
    return actions;
  }

  @Nullable
  public static Resource fromString(String name) {
    return EnumUtils.getEnum(Resource.class, name);
  }

  public List<PermissibleAction> parseActionsWithDependantsUnnest(List<String> actionsToParse) {
    return actionsToParse.stream()
        .map(toParse -> actions.stream()
            .filter(a -> toParse.equalsIgnoreCase(a.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "'%s' actions not applicable for resource %s".formatted(toParse, name())))
        )
        // unnesting all dependant actions
        .flatMap(a -> Stream.concat(Stream.of(a), a.unnestAllDependants()))
        .distinct()
        .toList();
  }

}
