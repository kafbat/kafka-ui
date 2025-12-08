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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    Collection<PermissibleAction> requestedActions();

    boolean isAccessible(List<Permission> userPermissions);
  }

  record SingleResourceAccess(@Nullable String name,
                              Resource resourceType,
                              Collection<PermissibleAction> requestedActions) implements ResourceAccess {

    SingleResourceAccess(@Nullable String name,
                         Resource resourceType,
                         Collection<PermissibleAction> requestedActions) {
      Preconditions.checkArgument(!requestedActions.isEmpty(), "actions not present");
      this.name = name;
      this.resourceType = resourceType;
      this.requestedActions = requestedActions;
    }

    SingleResourceAccess(Resource type, List<PermissibleAction> requestedActions) {
      this(null, type, requestedActions);
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

      return allowedActions.containsAll(requestedActions);
    }
  }

  /**
   * A ResourceAccess that checks primary first, then falls back to fallback if primary fails.
   * This enables OR semantics: access is granted if EITHER primary OR fallback is accessible.
   */
  record FallbackResourceAccess(
      ResourceAccess primary,
      ResourceAccess fallback
  ) implements ResourceAccess {

    @Override
    public Object resourceId() {
      return primary.resourceId();
    }

    @Override
    public Resource resourceType() {
      return primary.resourceType();
    }

    @Override
    public Collection<PermissibleAction> requestedActions() {
      return primary.requestedActions();
    }

    @Override
    public boolean isAccessible(List<Permission> userPermissions) {
      return primary.isAccessible(userPermissions)
          || fallback.isAccessible(userPermissions);
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
      List<ResourceAccess> finalResources = applyConnectorFallback(accessedResources);
      return new AccessContext(cluster, finalResources, operationName, operationParams);
    }

    private List<ResourceAccess> applyConnectorFallback(List<ResourceAccess> resources) {
      String connectorName = extractConnectorName();
      if (connectorName == null) {
        return resources;
      }

      List<ResourceAccess> result = new ArrayList<>();
      for (ResourceAccess resource : resources) {
        if (resource.resourceType() == Resource.CONNECT && resource instanceof SingleResourceAccess sra) {
          String connectName = sra.name();
          String connectorPath = connectName + "/" + connectorName;
          ConnectorAction[] connectorActions = mapConnectToConnectorActions(sra.requestedActions());

          ResourceAccess connectorAccess = new SingleResourceAccess(
              connectorPath, Resource.CONNECTOR, List.of(connectorActions));

          result.add(new FallbackResourceAccess(connectorAccess, resource));
        } else {
          result.add(resource);
        }
      }
      return result;
    }

    @Nullable
    private String extractConnectorName() {
      if (operationParams instanceof Map<?, ?> map) {
        Object value = map.get("connectorName");
        if (value instanceof String s) {
          return s;
        }
      }
      return null;
    }

    private ConnectorAction[] mapConnectToConnectorActions(Collection<PermissibleAction> actions) {
      return actions.stream()
          .filter(a -> a instanceof ConnectAction)
          .map(a -> mapSingleAction((ConnectAction) a))
          .distinct()
          .toArray(ConnectorAction[]::new);
    }

    private ConnectorAction mapSingleAction(ConnectAction action) {
      return switch (action) {
        case VIEW -> ConnectorAction.VIEW;
        case EDIT -> ConnectorAction.EDIT;
        case CREATE -> ConnectorAction.CREATE;
        case DELETE -> ConnectorAction.DELETE;
        case OPERATE -> ConnectorAction.OPERATE;
        case RESET_OFFSETS -> ConnectorAction.RESET_OFFSETS;
      };
    }
  }
}
