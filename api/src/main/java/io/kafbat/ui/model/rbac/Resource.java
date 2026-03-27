package io.kafbat.ui.model.rbac;

import io.kafbat.ui.model.rbac.permission.AclAction;
import io.kafbat.ui.model.rbac.permission.ApplicationConfigAction;
import io.kafbat.ui.model.rbac.permission.AuditAction;
import io.kafbat.ui.model.rbac.permission.ClientQuotaAction;
import io.kafbat.ui.model.rbac.permission.ClusterConfigAction;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConnectorAction;
import io.kafbat.ui.model.rbac.permission.ConsumerGroupAction;
import io.kafbat.ui.model.rbac.permission.KsqlAction;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;

public enum Resource {

  APPLICATIONCONFIG(ApplicationConfigAction.values()),

  CLUSTERCONFIG(ClusterConfigAction.values()),

  TOPIC(TopicAction.values()),

  CONSUMER(ConsumerGroupAction.values()),

  SCHEMA(SchemaAction.values()),

  CONNECT(ConnectAction.values(), ConnectAction.ALIASES),

  CONNECTOR(ConnectorAction.values()),

  KSQL(KsqlAction.values()),

  ACL(AclAction.values()),

  AUDIT(AuditAction.values()),

  CLIENT_QUOTAS(ClientQuotaAction.values());


  private final Map<String, PermissibleAction> actions;
  private final Map<String, PermissibleAction> aliases;

  Resource(PermissibleAction[] actions, Map<String, PermissibleAction> aliases) {
    this.actions = Arrays.stream(actions)
        .collect(
            Collectors.toMap(
                a -> a.name().toLowerCase(),
                a -> a
            )
        );
    this.aliases = aliases;
  }

  Resource(PermissibleAction[] actions) {
    this(actions, Map.of());
  }

  public List<PermissibleAction> allActions() {
    return new ArrayList<>(actions.values());
  }

  @Nullable
  public static Resource fromString(String name) {
    return EnumUtils.getEnum(Resource.class, name);
  }

  public List<PermissibleAction> parseActionsWithDependantsUnnest(List<String> actionsToParse) {
    return actionsToParse.stream().map(toParse ->
        Optional.ofNullable(actions.get(toParse.toLowerCase()))
            .or(() -> Optional.ofNullable(aliases.get(toParse.toLowerCase())))
            .orElseThrow(() -> new IllegalArgumentException(
                "'%s' actions not applicable for resource %s".formatted(toParse, name())))
    ).flatMap(a ->
            Stream.concat(Stream.of(a), a.unnestAllDependants())
    ).distinct().toList();
  }

}
