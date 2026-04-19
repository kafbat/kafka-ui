package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuthSecurityConfigRoleCheckTest {

  private AccessControlService acs;
  private OAuthSecurityConfig config;

  @BeforeEach
  void setUp() {
    acs = mock(AccessControlService.class);
    OAuthProperties props = mock(OAuthProperties.class);
    when(props.getClient()).thenReturn(java.util.Collections.emptyMap());
    config = new OAuthSecurityConfig(props);
  }

  @Test
  void whenRbacDisabled_userWithNoGroups_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(false);
    config.checkUserHasRoles(List.of(), acs);
  }

  @Test
  void whenRbacDisabled_userWithGroups_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(false);
    config.checkUserHasRoles(List.of("some-role"), acs);
  }

  @Test
  void whenRbacEnabled_userWithNoGroups_noDefaultRole_isDenied() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin")));
    when(acs.getDefaultRole()).thenReturn(null);
    assertThatThrownBy(() -> config.checkUserHasRoles(List.of(), acs))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void whenRbacEnabled_userGroupsDoNotMatchAnyRole_noDefaultRole_isDenied() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin"), roleNamed("dev")));
    when(acs.getDefaultRole()).thenReturn(null);
    assertThatThrownBy(() -> config.checkUserHasRoles(List.of("viewer"), acs))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void whenRbacEnabled_userGroupMatchesRole_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin"), roleNamed("dev")));
    config.checkUserHasRoles(List.of("dev"), acs);
  }

  @Test
  void whenRbacEnabled_userWithNoGroups_defaultRoleExists_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin")));
    when(acs.getDefaultRole()).thenReturn(new DefaultRole());
    config.checkUserHasRoles(List.of(), acs);
  }

  @Test
  void whenRbacEnabled_noRolesConfigured_defaultRoleExists_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of());
    when(acs.getDefaultRole()).thenReturn(new DefaultRole());
    config.checkUserHasRoles(List.of(), acs);
  }

  private Role roleNamed(String name) {
    Role r = new Role();
    r.setName(name);
    r.setClusters(List.of("test-cluster"));
    r.setSubjects(List.of());
    r.setPermissions(List.of());
    return r;
  }
}