package io.kafbat.ui.service.rbac;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for connector-level permissions in Kafka Connect.
 * Tests the hierarchical permission system where connector-level permissions
 * take precedence over connect-level permissions.
 */
@ActiveProfiles("rbac")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacConnectorPermissionsTest extends AbstractIntegrationTest {

  public static final String DEV_ROLE_NAME = "dev_role";
  public static final String ADMIN_ROLE_NAME = "admin_role";
  public static final String CLUSTER_NAME = "local";
  public static final String CONNECT_NAME = "kafka-connect";
  public static final String CONNECTOR_NAME = "my-connector";
  public static final String ANOTHER_CONNECTOR_NAME = "another-connector";

  @Autowired
  AccessControlService accessControlService;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  RbacUser user;

  @BeforeEach
  void setUp() {
    // Mock roles
    List<Role> roles = List.of(
        getDevRole(),
        getAdminRole()
    );
    RoleBasedAccessControlProperties properties = mock();
    when(properties.getRoles()).thenReturn(roles);

    ReflectionTestUtils.setField(accessControlService, "properties", properties);
    ReflectionTestUtils.setField(accessControlService, "rbacEnabled", true);

    // Mock security context
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
          .connectActions(CONNECT_NAME, ConnectAction.VIEW)
          .operationParams(Map.of("connectorName", CONNECTOR_NAME))
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
          .connectActions(CONNECT_NAME, ConnectAction.VIEW)
          .operationParams(Map.of("connectorName", ANOTHER_CONNECTOR_NAME))
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
          .connectActions(CONNECT_NAME, ConnectAction.VIEW)
          .operationParams(Map.of("connectorName", "any-connector-name"))
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
          .connectActions(CONNECT_NAME, ConnectAction.DELETE)
          .operationParams(Map.of("connectorName", CONNECTOR_NAME))
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
          .connectActions(CONNECT_NAME, ConnectAction.OPERATE)
          .operationParams(Map.of("connectorName", "any-connector"))
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
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
}
