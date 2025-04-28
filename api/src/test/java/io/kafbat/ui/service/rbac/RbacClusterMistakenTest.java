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
import io.kafbat.ui.model.rbac.permission.AclAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for issue #274.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacClusterMistakenTest extends AbstractIntegrationTest {

  public static final String ADMIN_ROLE_NAME = "Admin Roles";

  public static final String DEV_CLUSTER_ADM = "DEV";
  public static final String TST_CLUSTER_ADM = "TST";
  public static final String UAT_CLUSTER_ADM = "UAT";
  public static final String LAB_CLUSTER_ALL = "LAB";
  public static final String LAB_ROLE_NAME = "LAB for ALL";

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
        getAdminRole(),
        getDevRole()
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

  /**
   * Anyone editing LAB.
   */
  @Test
  void validateAccess() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(LAB_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(LAB_CLUSTER_ALL)
          .aclActions(AclAction.EDIT)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Admin with both roles editing LAB.
   */
  @Test
  void validateAccess_bothRoles() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE_NAME, LAB_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(LAB_CLUSTER_ALL)
          .aclActions(AclAction.EDIT)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Anyone editing Dev cluster, denied.
   */
  @Test
  void validateAccess_Denied() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(LAB_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(DEV_CLUSTER_ADM)
          .aclActions(AclAction.EDIT)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  /**
   * Admin editing dev cluster, denied.
   */
  @Test
  void validateAccess_DeniedAdminEditing() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(DEV_CLUSTER_ADM)
          .aclActions(AclAction.EDIT)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  /**
   * Admin viewing Dev cluster, allowed.
   */
  @Test
  void validateAccess_viewAllowedAdmin() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(DEV_CLUSTER_ADM)
          .aclActions(AclAction.VIEW)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  private Role getAdminRole() {
    Role role = new Role();
    role.setName(ADMIN_ROLE_NAME);
    role.setClusters(List.of(DEV_CLUSTER_ADM, TST_CLUSTER_ADM, UAT_CLUSTER_ADM));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission permission = new Permission();
    permission.setResource(Resource.ACL.name());
    permission.setActions(List.of(AclAction.VIEW.name()));

    List<Permission> permissions = List.of(
        permission
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

  private Role getDevRole() {
    Role role = new Role();
    role.setName(LAB_ROLE_NAME);
    role.setClusters(List.of(LAB_CLUSTER_ALL));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission permission = new Permission();
    permission.setResource(Resource.ACL.name());
    permission.setActions(List.of(AclAction.VIEW.name(), AclAction.EDIT.name()));

    List<Permission> permissions = List.of(
        permission
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

}
