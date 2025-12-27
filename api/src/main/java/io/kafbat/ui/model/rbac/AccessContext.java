package io.kafbat.ui.model.rbac;

import static io.kafbat.ui.model.rbac.permission.SchemaAction.MODIFY_GLOBAL_COMPATIBILITY;

import com.google.common.base.Preconditions;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;

public record AccessContext(String cluster,
                            List<ResourceAccess> accessedResources,
                            String operationName,
                            @Nullable Object operationParams) {

  public interface ResourceAccess {
    // will be used for audit, should be serializable via json object mapper
    @Nullable
    Object resourceId();

    Resource resourceType();

    Collection<? extends PermissibleAction> requestedActions();

    boolean isAccessible(List<Permission> userPermissions);

    @Nullable
    ResourceAccess fallback();
  }

  record SingleResourceAccess(@Nullable String name,
                              Resource resourceType,
                              Collection<? extends PermissibleAction> requestedActions,
                              @Nullable ResourceAccess fallback) implements ResourceAccess {

    SingleResourceAccess {
      Preconditions.checkArgument(!requestedActions.isEmpty(), "actions not present");
    }

    SingleResourceAccess(@Nullable String name, Resource type, List<? extends PermissibleAction> requestedActions) {
      this(name, type, requestedActions, null);
    }

    SingleResourceAccess(Resource type, List<? extends PermissibleAction> requestedActions, ResourceAccess fallback) {
      this(null, type, requestedActions, fallback);
    }

    SingleResourceAccess(Resource type, List<? extends PermissibleAction> requestedActions) {
      this(null, type, requestedActions, null);
    }

    @Override
    public Object resourceId() {
      return name;
    }

    @Override
    public boolean isAccessible(List<Permission> userPermissions) throws AccessDeniedException {
      var allowedActions = userPermissions.stream()
          .filter(permission -> permission.getResource() == resourceType)
          .filter(permission -> {
            if (name == null && permission.getCompiledValuePattern() == null) {
              return true;
            }
            if (permission.getCompiledValuePattern() != null && name != null) {
              return permission.getCompiledValuePattern().matcher(name).matches();
            }
            return false;
          })
          .flatMap(p -> p.getParsedActions().stream())
          .collect(Collectors.toSet());

      return allowedActions.containsAll(requestedActions)
          || Optional.ofNullable(fallback).map(e -> e.isAccessible(userPermissions)).orElse(false);
    }
  }

  public static AccessContextBuilder builder() {
    return new AccessContextBuilder();
  }

  public boolean isAccessible(List<Permission> userPermissions) {
    return accessedResources().stream()
        .allMatch(resourceAccess -> resourceAccess.isAccessible(userPermissions));
  }

  public static final class AccessContextBuilder {

    private String cluster;
    private String operationName;
    private Object operationParams;
    private final List<ResourceAccess> accessedResources = new ArrayList<>();

    private AccessContextBuilder() {
    }

    public AccessContextBuilder cluster(String cluster) {
      this.cluster = cluster;
      return this;
    }

    public AccessContextBuilder applicationConfigActions(ApplicationConfigAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.APPLICATIONCONFIG, List.of(actions)));
      return this;
    }

    public AccessContextBuilder clusterConfigActions(ClusterConfigAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.CLUSTERCONFIG, List.of(actions)));
      return this;
    }

    public AccessContextBuilder topicActions(String topic, TopicAction... actions) {
      accessedResources.add(new SingleResourceAccess(topic, Resource.TOPIC, List.of(actions)));
      return this;
    }

    public AccessContextBuilder consumerGroupActions(String consumerGroup, ConsumerGroupAction... actions) {
      accessedResources.add(new SingleResourceAccess(consumerGroup, Resource.CONSUMER, List.of(actions)));
      return this;
    }

    public AccessContextBuilder connectActions(String connect, ConnectAction... actions) {
      accessedResources.add(new SingleResourceAccess(connect, Resource.CONNECT, List.of(actions)));
      return this;
    }

    public AccessContextBuilder connectorActions(String connect, String connector, ConnectorAction... actions) {
      accessedResources.add(
          new SingleResourceAccess(String.join("/", connect, connector), Resource.CONNECTOR, List.of(actions),
              new SingleResourceAccess(
                  connect, Resource.CONNECT,
                  Stream.of(actions).map(ConnectorAction::getConnectAction).toList()
              )
          )
      );
      return this;
    }

    public AccessContextBuilder schemaActions(String schema, SchemaAction... actions) {
      accessedResources.add(new SingleResourceAccess(schema, Resource.SCHEMA, List.of(actions)));
      return this;
    }

    public AccessContextBuilder schemaGlobalCompatChange() {
      accessedResources.add(new SingleResourceAccess(Resource.SCHEMA, List.of(MODIFY_GLOBAL_COMPATIBILITY)));
      return this;
    }

    public AccessContextBuilder ksqlActions(KsqlAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.KSQL, List.of(actions)));
      return this;
    }

    public AccessContextBuilder aclActions(AclAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.ACL, List.of(actions)));
      return this;
    }

    public AccessContextBuilder auditActions(AuditAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.AUDIT, List.of(actions)));
      return this;
    }

    public AccessContextBuilder clientQuotaActions(ClientQuotaAction... actions) {
      accessedResources.add(new SingleResourceAccess(Resource.CLIENT_QUOTAS, List.of(actions)));
      return this;
    }

    public AccessContextBuilder operationName(String operationName) {
      this.operationName = operationName;
      return this;
    }

    public AccessContextBuilder operationParams(Object operationParams) {
      this.operationParams = operationParams;
      return this;
    }

    public AccessContextBuilder operationParams(Map<String, Object> paramsMap) {
      this.operationParams = paramsMap;
      return this;
    }

    public AccessContext build() {
      return new AccessContext(cluster, accessedResources, operationName, operationParams);
    }
  }
}
