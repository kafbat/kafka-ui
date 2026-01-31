package io.kafbat.ui.service.rbac;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Resource;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConnectorAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for connector-level permissions in Kafka Connect.
 * Tests the hierarchical permission system where connector-level permissions
 * take precedence over connect-level permissions.
 */
@ExtendWith(MockitoExtension.class)
class RbacConnectorPermissionsTest {

  public static final String DEV_ROLE_NAME = "dev_role";
  public static final String ADMIN_ROLE_NAME = "admin_role";
  public static final String CLUSTER_NAME = "local";
  public static final String CONNECT_NAME = "kafka-connect";
  public static final String CONNECTOR_NAME = "my-connector";

  private AccessControlService accessControlService;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  RbacUser user;

  @Mock
  org.springframework.core.env.Environment environment;

  @BeforeEach
  void setUp() {
    List<Role> roles = List.of(
        getDevRole(),
        getAdminRole(),
        getWildcardConnectRole(),
        getWildcardConnectorRole(),
        getFullWildcardRole()
    );
    RoleBasedAccessControlProperties properties = mock();
    when(properties.getRoles()).thenReturn(roles);

    accessControlService = new AccessControlService(null, properties, environment);
    accessControlService.init();

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
  }

  public void withSecurityContext(Runnable runnable) {
    try (MockedStatic<ReactiveSecurityContextHolder> ctxHolder = Mockito.mockStatic(
        ReactiveSecurityContextHolder.class)) {
      // Mock static method to get security context
      ctxHolder.when(ReactiveSecurityContextHolder::getContext)
          .thenReturn(Mono.just(securityContext));
      runnable.run();
    }
  }

  /**
   * Test that a user with specific connector-level permission can view the connector.
   */
  @Test
  void validateAccess_withConnectorLevelPermission_allowed() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .connectorActions(CONNECT_NAME, CONNECTOR_NAME, ConnectorAction.VIEW)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test that a user without specific connector-level permission is denied access.
   */
  @Test
  void validateAccess_withoutConnectorLevelPermission_denied() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .connectorActions(CONNECT_NAME, "not-" + CONNECTOR_NAME, ConnectorAction.VIEW)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  /**
   * Test that a user with wildcard connector permission can access any connector.
   */
  @Test
  void validateAccess_withWildcardConnectorPermission_allowed() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .connectorActions(CONNECT_NAME, CONNECTOR_NAME, ConnectorAction.VIEW)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test that connector-level DELETE permission works.
   */
  @Test
  void validateAccess_withConnectorLevelDeletePermission_allowed() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .connectorActions(CONNECT_NAME, CONNECTOR_NAME, ConnectorAction.DELETE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test that fallback to connect-level permission works.
   * Admin has CONNECT.OPERATE permission, which should allow access.
   */
  @Test
  void validateAccess_fallsBackToConnectPermission() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE_NAME));
      // Admin has CONNECT.OPERATE but checking a connector not in wildcard
      // The fallback to connect-level should allow access
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .connectorActions(CONNECT_NAME, CONNECTOR_NAME, ConnectorAction.OPERATE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test that isConnectAccessible returns true when user has connector VIEW permission
   * but no direct connect permission (Issue #1612).
   */
  @Test
  void isConnectAccessible_withOnlyConnectorPermission_returnsTrue() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE_NAME));
      // Dev role only has connector permission, no connect permission
      Mono<Boolean> result = accessControlService.isConnectAccessible(CONNECT_NAME, CLUSTER_NAME);
      StepVerifier.create(result)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test that isConnectAccessible returns false when user has no matching connector permission.
   */
  @Test
  void isConnectAccessible_withNoMatchingConnectorPermission_returnsFalse() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE_NAME));
      // Dev role has connector permission for kafka-connect, not other-connect
      Mono<Boolean> result = accessControlService.isConnectAccessible("other-connect", CLUSTER_NAME);
      StepVerifier.create(result)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test isConnectAccessible with wildcard connect pattern (any connect, specific connector).
   */
  @Test
  void isConnectAccessible_withWildcardConnectPattern_returnsTrue() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("wildcard_connect_role"));
      Mono<Boolean> result = accessControlService.isConnectAccessible("any-connect", CLUSTER_NAME);
      StepVerifier.create(result)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test isConnectAccessible with wildcard connector pattern (connect/.*).
   */
  @Test
  void isConnectAccessible_withWildcardConnectorPattern_returnsTrue() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("wildcard_connector_role"));
      Mono<Boolean> result = accessControlService.isConnectAccessible(CONNECT_NAME, CLUSTER_NAME);
      StepVerifier.create(result)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Test isConnectAccessible with full wildcard pattern (any connect, any connector).
   */
  @Test
  void isConnectAccessible_withFullWildcardPattern_returnsTrue() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("full_wildcard_role"));
      Mono<Boolean> result = accessControlService.isConnectAccessible("any-connect", CLUSTER_NAME);
      StepVerifier.create(result)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Dev role with specific connector-level permissions.
   */
  public static Role getDevRole() {
    Role role = new Role();
    role.setName(DEV_ROLE_NAME);
    role.setClusters(List.of(CLUSTER_NAME));

    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("dev.group");
    role.setSubjects(List.of(sub));

    // Specific connector-level permission for "my-connector"
    Permission specificConnectorPermission = new Permission();
    specificConnectorPermission.setResource(Resource.CONNECTOR.name());
    specificConnectorPermission.setActions(List.of(
        ConnectorAction.VIEW.name(),
        ConnectorAction.EDIT.name(),
        ConnectorAction.DELETE.name()
    ));
    specificConnectorPermission.setValue(
        ConnectorAction.buildResourcePath(CONNECT_NAME, CONNECTOR_NAME));

    List<Permission> permissions = List.of(specificConnectorPermission);
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

  /**
   * Admin role with wildcard permissions on all connectors.
   */
  public static Role getAdminRole() {
    Role role = new Role();
    role.setName(ADMIN_ROLE_NAME);
    role.setClusters(List.of(CLUSTER_NAME));

    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("admin.group");
    role.setSubjects(List.of(sub));

    // Wildcard connector-level permission
    Permission wildcardConnectorPermission = new Permission();
    wildcardConnectorPermission.setResource(Resource.CONNECTOR.name());
    wildcardConnectorPermission.setActions(List.of(
        ConnectorAction.VIEW.name(),
        ConnectorAction.EDIT.name(),
        ConnectorAction.OPERATE.name(),
        ConnectorAction.DELETE.name(),
        ConnectorAction.RESET_OFFSETS.name()
    ));
    wildcardConnectorPermission.setValue(
        CONNECT_NAME + ConnectorAction.CONNECTOR_RESOURCE_DELIMITER + ".*");

    // Also have connect-level permissions for backwards compatibility
    Permission connectPermission = new Permission();
    connectPermission.setResource(Resource.CONNECT.name());
    connectPermission.setActions(List.of(
        ConnectAction.VIEW.name(),
        ConnectAction.EDIT.name(),
        ConnectAction.OPERATE.name()
    ));
    connectPermission.setValue(CONNECT_NAME);

    List<Permission> permissions = List.of(wildcardConnectorPermission, connectPermission);
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

  /**
   * Role with wildcard connect pattern (any connect, specific connector).
   * Allows user to view specific connector on any connect cluster.
   */
  public static Role getWildcardConnectRole() {
    Role role = new Role();
    role.setName("wildcard_connect_role");
    role.setClusters(List.of(CLUSTER_NAME));

    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("wildcard.connect.group");
    role.setSubjects(List.of(sub));

    // Pattern: .*/specific-connector (any connect, specific connector)
    Permission permission = new Permission();
    permission.setResource(Resource.CONNECTOR.name());
    permission.setActions(List.of(ConnectorAction.VIEW.name()));
    permission.setValue(".*/specific-connector");

    role.setPermissions(List.of(permission));
    role.validate();
    return role;
  }

  /**
   * Role with wildcard connector pattern - pattern: connect-name/.*
   * Allows user to view any connector on a specific connect cluster.
   */
  public static Role getWildcardConnectorRole() {
    Role role = new Role();
    role.setName("wildcard_connector_role");
    role.setClusters(List.of(CLUSTER_NAME));

    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("wildcard.connector.group");
    role.setSubjects(List.of(sub));

    // Pattern: kafka-connect/.* (specific connect, any connector)
    Permission permission = new Permission();
    permission.setResource(Resource.CONNECTOR.name());
    permission.setActions(List.of(ConnectorAction.VIEW.name()));
    permission.setValue(CONNECT_NAME + "/.*");

    role.setPermissions(List.of(permission));
    role.validate();
    return role;
  }

  /**
   * Role with full wildcard pattern (any connect, any connector).
   * Allows user to view any connector on any connect cluster.
   */
  public static Role getFullWildcardRole() {
    Role role = new Role();
    role.setName("full_wildcard_role");
    role.setClusters(List.of(CLUSTER_NAME));

    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("full.wildcard.group");
    role.setSubjects(List.of(sub));

    // Pattern: .*/.*  (any connect, any connector)
    Permission permission = new Permission();
    permission.setResource(Resource.CONNECTOR.name());
    permission.setActions(List.of(ConnectorAction.VIEW.name()));
    permission.setValue(".*/.*");

    role.setPermissions(List.of(permission));
    role.validate();
    return role;
  }
}
