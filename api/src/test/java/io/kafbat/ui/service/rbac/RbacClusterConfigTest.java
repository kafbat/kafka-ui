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
import io.kafbat.ui.model.rbac.permission.ClusterConfigAction;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for issue #461.
 * Sets the role to any cluster ".*"
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacClusterConfigTest extends AbstractIntegrationTest {

  public static final String ROLE_NAME = "cluster_config_role";

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
        getClusterConfigRole()
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
      ctxHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
      runnable.run();
    }
  }

  @Test
  @Disabled("expected to work after issue #461 is resolved")
  void validateAccess_clusterConfigAll_propertiesAllCluster() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster("prod")
          .clusterConfigActions(ClusterConfigAction.EDIT)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  public static Role getClusterConfigRole() {
    Role role = new Role();
    role.setName(ROLE_NAME);
    role.setClusters(List.of(".*")); // setting role for any cluster

    Permission permission = new Permission();
    permission.setResource(Resource.CLUSTERCONFIG.name());
    permission.setActions(List.of("all"));

    List<Permission> permissions = List.of(
        permission
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

}
