package io.kafbat.ui.service.rbac;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Resource;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConsumerGroupAction;
import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.List;

public class MockedRbacUtils {

  public static final String ADMIN_ROLE = "admin_role";
  public static final String DEV_ROLE = "dev_role";

  public static final String PROD_CLUSTER = "prod";
  public static final String DEV_CLUSTER = "dev";

  public static final String TOPIC_NAME = "aTopic";
  public static final String CONSUMER_GROUP_NAME = "aConsumerGroup";
  public static final String SCHEMA_NAME = "aSchema";
  public static final String CONNECT_NAME = "aConnect";

  /**
   * All actions to Resource.APPLICATIONCONFIG for dev and prod clusters.
   *
   * @return admin role
   */
  public static Role getAdminRole() {
    Role role = new Role();
    role.setName(ADMIN_ROLE);
    role.setClusters(List.of(DEV_CLUSTER, PROD_CLUSTER));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission applicationConfigPerm = new Permission();
    applicationConfigPerm.setResource(Resource.APPLICATIONCONFIG.name());
    applicationConfigPerm.setActions(List.of("all"));
    List<Permission> permissions = List.of(
        applicationConfigPerm
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

  /**
   * View actions to topic, consumer, schema and connect.
   *
   * @return admin role
   */
  public static Role getDevRole() {
    Role role = new Role();
    role.setName(DEV_ROLE);
    role.setClusters(List.of(DEV_CLUSTER));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission topicViewPermission = new Permission();
    topicViewPermission.setResource(Resource.TOPIC.name());
    topicViewPermission.setActions(List.of(TopicAction.VIEW.name()));
    topicViewPermission.setValue(TOPIC_NAME);

    Permission consumerGroupPermission = new Permission();
    consumerGroupPermission.setResource(Resource.CONSUMER.name());
    consumerGroupPermission.setActions(List.of(ConsumerGroupAction.VIEW.name()));
    consumerGroupPermission.setValue(CONSUMER_GROUP_NAME);

    Permission schemaPermission = new Permission();
    schemaPermission.setResource(Resource.SCHEMA.name());
    schemaPermission.setActions(List.of(SchemaAction.VIEW.name()));
    schemaPermission.setValue(SCHEMA_NAME);

    Permission connectPermission = new Permission();
    connectPermission.setResource(Resource.CONNECT.name());
    connectPermission.setActions(List.of(ConnectAction.VIEW.name()));
    connectPermission.setValue(CONNECT_NAME);

    List<Permission> permissions = List.of(
        topicViewPermission,
        consumerGroupPermission,
        schemaPermission,
        connectPermission
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

  public static AccessContext getAccessContext(String cluster, Boolean resourceAccessible) {
    AccessContext.ResourceAccess mockedResource = mock(AccessContext.ResourceAccess.class);
    when(mockedResource.isAccessible(any())).thenReturn(resourceAccessible);
    return new AccessContext(cluster, List.of(
        mockedResource
    ), "op", "params");
  }

}
