package io.kafbat.ui.controller;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.config.auth.RbacLdapUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.ActionDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.permission.ApplicationConfigAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class AuthorizationControllerDefaultRoleTest {

  @Test
  void reportsDefaultPermissionsForClustersWithoutAnAssignedRole() {
    var fixture = fixture(List.of(role(List.of(permission("team-a-.*")))), defaultRole());
    requirePermission(fixture, "cluster-a", "team-a-events", true);
    requirePermission(fixture, "cluster-b", "shared-events", true);
    requirePermission(fixture, "cluster-a", "shared-events", false);
  }

  @Test
  void doesNotReplaceEmptyExplicitRoleWithDefaultPermissions() {
    var fixture = fixture(List.of(role(List.of())), defaultRole());
    requirePermission(fixture, "cluster-a", "shared-events", false);
    requirePermission(fixture, "cluster-b", "shared-events", true);
  }

  @Test
  void reportsDefaultPermissionsWhenNoRolesMatch() {
    var fixture = fixture(List.of(), defaultRole());
    requirePermission(fixture, "cluster-a", "shared-events", true);
    requirePermission(fixture, "cluster-b", "shared-events", true);
  }

  @Test
  void preservesExplicitPermissionsWithoutDefaultRole() {
    var fixture = fixture(List.of(role(List.of(permission("team-a-.*")))), null);
    requirePermission(fixture, "cluster-a", "team-a-events", true);
    requirePermission(fixture, "cluster-b", "team-a-events", false);
  }

  @Test
  void doesNotReportDefaultGlobalPermissionsWhenARoleMatches() {
    var defaultRole = defaultRole();
    defaultRole.getPermissions().add(applicationConfigPermission());
    var fixture = fixture(List.of(role(List.of(permission("team-a-.*")))), defaultRole);
    requireApplicationConfigPermission(fixture, false);
    requirePermission(fixture, "cluster-b", "shared-events", true);
  }

  @Test
  void doesNotReportDefaultGlobalPermissionsForAnEmptyExplicitRole() {
    var defaultRole = defaultRole();
    defaultRole.getPermissions().add(applicationConfigPermission());
    var fixture = fixture(List.of(role(List.of())), defaultRole);
    requireApplicationConfigPermission(fixture, false);
  }

  @Test
  void reportsDefaultGlobalPermissionsWhenNoRolesMatch() {
    var defaultRole = defaultRole();
    defaultRole.getPermissions().add(applicationConfigPermission());
    var fixture = fixture(List.of(), defaultRole);
    requireApplicationConfigPermission(fixture, true);
  }

  @Test
  void preservesExplicitGlobalPermissions() {
    var fixture = fixture(List.of(role(List.of(applicationConfigPermission()))), defaultRole());
    requireApplicationConfigPermission(fixture, true);
    requirePermission(fixture, "cluster-b", "shared-events", true);
  }

  @Test
  void doesNotExposeDefaultPermissionsToAnonymousRequests() {
    var fixture = fixture(List.of(), defaultRole());
    var response = fixture.controller().getUserAuthInfo(null).block();
    if (response.getBody().getUserInfo() != null) {
      throw new AssertionError("Anonymous responses must not include user permissions");
    }
  }

  private void requireApplicationConfigPermission(Fixture fixture, boolean expected) {
    boolean backend = fixture.acs().validateAccess(AccessContext.builder()
            .applicationConfigActions(ApplicationConfigAction.VIEW).build())
        .thenReturn(true)
        .onErrorReturn(AccessDeniedException.class, false)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(fixture.authentication()))
        .block();
    if (backend != expected) {
      throw new AssertionError("Unexpected backend application config permission: " + backend);
    }

    var response = fixture.controller().getUserAuthInfo(null)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(fixture.authentication()))
        .block();
    boolean reported = response.getBody().getUserInfo().getPermissions().stream()
        .anyMatch(permission -> permission.getResource() == ResourceTypeDTO.APPLICATIONCONFIG
            && permission.getActions().contains(ActionDTO.VIEW));
    if (reported != expected) {
      throw new AssertionError("Reported application config permission differs from backend: expected "
          + expected + ", actual " + reported);
    }
  }

  private void requirePermission(Fixture fixture, String cluster, String topic, boolean expected) {
    boolean backend = fixture.acs().validateAccess(AccessContext.builder()
            .cluster(cluster).topicActions(topic, TopicAction.VIEW).build())
        .thenReturn(true)
        .onErrorReturn(AccessDeniedException.class, false)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(fixture.authentication()))
        .block();
    if (backend != expected) {
      throw new AssertionError("Unexpected backend permission for " + cluster + ": " + backend);
    }

    var response = fixture.controller().getUserAuthInfo(null)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(fixture.authentication()))
        .block();
    boolean reported = response.getBody().getUserInfo().getPermissions().stream()
        .filter(permission -> permission.getClusters().contains(cluster))
        .filter(permission -> permission.getResource() == ResourceTypeDTO.TOPIC)
        .filter(permission -> permission.getActions().contains(ActionDTO.VIEW))
        .anyMatch(permission -> permission.getValue() != null
            && Pattern.matches(permission.getValue(), topic));
    if (reported != expected) {
      throw new AssertionError("Reported permission differs from backend for " + cluster
          + ": expected " + expected + ", actual " + reported);
    }
  }

  private Fixture fixture(List<Role> roles, DefaultRole defaultRole) {
    var properties = new RoleBasedAccessControlProperties();
    properties.setRoles(roles);
    properties.setDefaultRole(defaultRole);
    properties.init();
    var acs = new AccessControlService(null, properties, new StandardEnvironment());
    acs.init();

    var user = new RbacLdapUser(User.withUsername("alice").password("test-only").authorities("role-a").build());
    var authentication = new UsernamePasswordAuthenticationToken(user, "test-only", user.getAuthorities());
    return new Fixture(acs, new AuthorizationController(acs, new LocalClustersStorage()), authentication);
  }

  private Role role(List<Permission> permissions) {
    var subject = new Subject();
    subject.setProvider(Provider.LDAP);
    subject.setType("user");
    subject.setValue("alice");
    var role = new Role();
    role.setName("role-a");
    role.setClusters(List.of("cluster-a"));
    role.setSubjects(List.of(subject));
    role.setPermissions(permissions);
    return role;
  }

  private Permission permission(String value) {
    var permission = new Permission();
    permission.setResource("topic");
    permission.setValue(value);
    permission.setActions(List.of("view"));
    return permission;
  }

  private DefaultRole defaultRole() {
    var role = new DefaultRole();
    role.getPermissions().add(permission("shared-.*"));
    return role;
  }

  private Permission applicationConfigPermission() {
    var permission = new Permission();
    permission.setResource("applicationconfig");
    permission.setActions(List.of("view"));
    return permission;
  }

  private record Fixture(AccessControlService acs, AuthorizationController controller, Authentication authentication) {
  }

  private static class LocalClustersStorage extends ClustersStorage {
    LocalClustersStorage() {
      super(new ClustersProperties(), null);
    }

    @Override
    public Collection<KafkaCluster> getKafkaClusters() {
      return List.of(KafkaCluster.builder().name("cluster-a").build(),
          KafkaCluster.builder().name("cluster-b").build());
    }
  }
}
