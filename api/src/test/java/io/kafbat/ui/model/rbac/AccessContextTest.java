package io.kafbat.ui.model.rbac;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.rbac.AccessContext.ResourceAccess;
import io.kafbat.ui.model.rbac.AccessContext.SingleResourceAccess;
import io.kafbat.ui.model.rbac.permission.ClusterConfigAction;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConnectorAction;
import io.kafbat.ui.model.rbac.permission.PermissibleAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccessContextTest {

  @Test
  void validateReturnsTrueIfAllResourcesAreAccessible() {
    ResourceAccess okResourceAccess1 = mock(ResourceAccess.class);
    when(okResourceAccess1.isAccessible(any())).thenReturn(true);

    ResourceAccess okResourceAccess2 = mock(ResourceAccess.class);
    when(okResourceAccess2.isAccessible(any())).thenReturn(true);

    var cxt = new AccessContext("cluster", List.of(okResourceAccess1, okResourceAccess2), "op", "params");
    assertThat(cxt.isAccessible(List.of())).isTrue();
  }

  @Test
  void validateReturnsFalseIfAnyResourcesCantBeAccessible() {
    ResourceAccess okResourceAccess = mock(ResourceAccess.class);
    when(okResourceAccess.isAccessible(any())).thenReturn(true);

    ResourceAccess failureResourceAccess = mock(ResourceAccess.class);
    when(failureResourceAccess.isAccessible(any())).thenReturn(false);

    var cxt = new AccessContext("cluster", List.of(okResourceAccess, failureResourceAccess), "op", "params");
    assertThat(cxt.isAccessible(List.of())).isFalse();
  }


  @Nested
  class SingleResourceAccessTest {

    public static final String CONNECT_NAME = "my-connect";
    public static final String CONNECTOR_NAME = "my-connector";
    public static final String FULL_CONNECTOR_NAME = String.join("/", CONNECT_NAME, CONNECTOR_NAME);

    @Test
    void allowsAccessForResourceWithNameIfUserHasAllNeededPermissions() {
      SingleResourceAccess sra =
          new SingleResourceAccess("test_topic123", Resource.TOPIC, List.of(TopicAction.VIEW, TopicAction.EDIT));

      var allowed = sra.isAccessible(
          List.of(
              permission(Resource.TOPIC, "test_topic.*", TopicAction.EDIT),
              permission(Resource.TOPIC, "test.*", TopicAction.VIEW)));

      assertThat(allowed).isTrue();
    }

    @Test
    void deniesAccessForResourceWithNameIfUserHasSomePermissionsMissing() {
      SingleResourceAccess sra =
          new SingleResourceAccess("test_topic123", Resource.TOPIC,
              List.of(TopicAction.VIEW, TopicAction.MESSAGES_DELETE));

      var allowed = sra.isAccessible(
          List.of(
              permission(Resource.TOPIC, "test_topic.*", TopicAction.EDIT),
              permission(Resource.TOPIC, "test.*", TopicAction.VIEW)));

      assertThat(allowed).isFalse();
    }

    @Test
    void allowsAccessForResourceWithoutNameIfUserHasAllNeededPermissions() {
      SingleResourceAccess sra =
          new SingleResourceAccess(Resource.CLUSTERCONFIG, List.of(ClusterConfigAction.VIEW));

      var allowed = sra.isAccessible(
          List.of(
              permission(Resource.CLUSTERCONFIG, null, ClusterConfigAction.VIEW, ClusterConfigAction.EDIT)));

      assertThat(allowed).isTrue();
    }

    @Test
    void deniesAccessForResourceWithoutNameIfUserHasAllNeededPermissions() {
      SingleResourceAccess sra =
          new SingleResourceAccess(Resource.CLUSTERCONFIG, List.of(ClusterConfigAction.EDIT));

      var allowed = sra.isAccessible(
          List.of(
              permission(Resource.CLUSTERCONFIG, null, ClusterConfigAction.VIEW)));

      assertThat(allowed).isFalse();
    }

    @Test
    void shouldMapActionAliases() {
      SingleResourceAccess sra =
          new SingleResourceAccess(Resource.CONNECT, List.of(ConnectAction.OPERATE));

      var allowed = sra.isAccessible(
          List.of(
              permission(Resource.CONNECT, null, List.of("restart"))
          )
      );

      assertThat(allowed).isTrue();
    }

    @Test
    void allowsAccessForConnectorWithSpecificNameIfUserHasPermissionToConnectOrConnector() {
      SingleResourceAccess sra = new SingleResourceAccess(
          FULL_CONNECTOR_NAME, Resource.CONNECTOR, List.of(ConnectorAction.VIEW, ConnectorAction.OPERATE),
          new SingleResourceAccess(CONNECT_NAME, Resource.CONNECT, List.of(ConnectAction.VIEW, ConnectAction.OPERATE))
      );

      assertThat(sra.isAccessible(
          List.of(
              permission(
                  Resource.CONNECTOR, FULL_CONNECTOR_NAME,
                  ConnectorAction.VIEW, ConnectorAction.OPERATE
              )
          )
      )).isTrue();

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECT, CONNECT_NAME, ConnectAction.VIEW, ConnectAction.OPERATE)
          )
      )).isTrue();

    }

    @Test
    void allowsAccessForConnectorWithWildcardPatternIfUserHasPermission() {
      SingleResourceAccess sra = new SingleResourceAccess(
          FULL_CONNECTOR_NAME, Resource.CONNECTOR, List.of(ConnectorAction.VIEW),
          new SingleResourceAccess(CONNECT_NAME, Resource.CONNECT, List.of(ConnectAction.VIEW))
      );

      assertThat(sra.isAccessible(
          List.of(
              permission(
                  Resource.CONNECTOR, CONNECT_NAME + "/.*",
                  ConnectorAction.VIEW, ConnectorAction.EDIT
              )
          )
      )).isTrue();

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECT, CONNECT_NAME, ConnectAction.VIEW, ConnectAction.EDIT)))
      ).isTrue();
    }

    @Test
    void deniesAccessForConnectorIfUserLacksRequiredPermission() {
      SingleResourceAccess sra = new SingleResourceAccess(
          FULL_CONNECTOR_NAME, Resource.CONNECTOR, List.of(ConnectorAction.DELETE),
          new SingleResourceAccess(CONNECT_NAME, Resource.CONNECT, List.of(ConnectAction.DELETE))
      );

      assertThat(sra.isAccessible(
          List.of(
              permission(
                  Resource.CONNECTOR, FULL_CONNECTOR_NAME,
                  ConnectorAction.VIEW, ConnectorAction.EDIT
              )
          )
      )).isFalse();

      assertThat(sra.isAccessible(
          List.of(
              permission(
                  Resource.CONNECT, CONNECT_NAME,
                  ConnectAction.VIEW, ConnectAction.EDIT
              )
          )
      )).isFalse();

    }

    @Test
    void allowsAccessForConnectorWithMultipleWildcardPatterns() {
      SingleResourceAccess sra = new SingleResourceAccess(
          FULL_CONNECTOR_NAME, Resource.CONNECTOR, List.of(ConnectorAction.RESET_OFFSETS),
          new SingleResourceAccess(CONNECT_NAME, Resource.CONNECT, List.of(ConnectAction.RESET_OFFSETS))
      );

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECTOR, ".*/my-.*", ConnectorAction.RESET_OFFSETS),
              permission(Resource.CONNECTOR, "my-.*/.*", ConnectorAction.VIEW)
          )
      )).isTrue();

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECT, "my-.*", ConnectorAction.RESET_OFFSETS),
              permission(Resource.CONNECT, ".*", ConnectorAction.VIEW)
          )
      )).isTrue();
    }

    @Test
    void testConnectorActionHierarchy() {
      // Test that EDIT includes VIEW permission
      SingleResourceAccess sra = new SingleResourceAccess(
          FULL_CONNECTOR_NAME, Resource.CONNECTOR, List.of(ConnectorAction.VIEW),
          new SingleResourceAccess(CONNECT_NAME, Resource.CONNECT, List.of(ConnectAction.VIEW))
      );

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECTOR, CONNECT_NAME + "/.*", ConnectorAction.VIEW)
          )
      )).isTrue();

      assertThat(sra.isAccessible(
          List.of(
              permission(Resource.CONNECT, CONNECT_NAME, ConnectAction.VIEW)
          )
      )).isTrue();
    }

    private Permission permission(Resource res, @Nullable String namePattern, PermissibleAction... actions) {
      return permission(
          res, namePattern, Stream.of(actions).map(PermissibleAction::name).toList()
      );
    }

    private Permission permission(Resource res, @Nullable String namePattern, List<String> actions) {
      Permission p = new Permission();
      p.setResource(res.name());
      p.setActions(actions);
      p.setValue(namePattern);
      p.validate();
      p.transform();
      return p;
    }
  }

}
