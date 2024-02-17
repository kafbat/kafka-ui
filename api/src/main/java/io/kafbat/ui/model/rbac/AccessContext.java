package io.kafbat.ui.model.rbac;

import com.google.common.base.Preconditions;
import io.kafbat.ui.model.rbac.permission.AclAction;
import io.kafbat.ui.model.rbac.permission.ApplicationConfigAction;
import io.kafbat.ui.model.rbac.permission.AuditAction;
import io.kafbat.ui.model.rbac.permission.ClusterConfigAction;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConsumerGroupAction;
import io.kafbat.ui.model.rbac.permission.KsqlAction;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
            Preconditions.checkState(permission.getCompiledValuePattern() != null && name != null);
            return permission.getCompiledValuePattern().matcher(name).matches();
          })
          .flatMap(p -> p.getParsedActions().stream())
          .collect(Collectors.toSet());

      return allowedActions.containsAll(requestedActions);
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
      accessedResources.add(new SingleResourceAccess(Resource.SCHEMA, List.of(SchemaAction.MODIFY_GLOBAL_COMPATIBILITY)));
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

    public AccessContextBuilder operationName(String operationName) {
      this.operationName = operationName;
      return this;
    }

    public AccessContextBuilder operationParams(Object operationParams) {
      this.operationParams = operationParams;
      return this;
    }

    public AccessContext build() {
      return new AccessContext(cluster, accessedResources, operationName, operationParams);
    }
  }
}
