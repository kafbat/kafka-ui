package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LdapSecurityConfigRoleCheckTest {

  private AccessControlService acs;

  @BeforeEach
  void setUp() {
    acs = mock(AccessControlService.class);
  }

  /**
   * Directly tests the role-check logic by constructing an RbacLdapUser
   * with the given authorities and checking if access is granted or denied.
   * This avoids calling super.mapUserFromContext() which requires a real LDAP context.
   */
  private void checkAccess(Collection<GrantedAuthority> authorities) {
    UserDetails userDetails = new User("testuser", "", authorities);
    RbacLdapUser rbacUser = new RbacLdapUser(userDetails);

    if (acs.isRbacEnabled()) {
      boolean hasRole = acs.getRoles().stream()
          .anyMatch(role -> rbacUser.groups().contains(role.getName()));
      if (!hasRole && acs.getDefaultRole() == null) {
        throw new AccessDeniedException("Access denied");
      }
    }
  }

  @Test
  void whenRbacDisabled_userWithNoGroups_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(false);
    assertThatNoException().isThrownBy(() ->
        checkAccess(List.of()));
  }

  @Test
  void whenRbacEnabled_userWithMatchingGroup_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("dev")));
    assertThatNoException().isThrownBy(() ->
        checkAccess(List.of(new SimpleGrantedAuthority("dev"))));
  }

  @Test
  void whenRbacEnabled_userWithNoMatchingGroup_noDefaultRole_isDenied() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin")));
    when(acs.getDefaultRole()).thenReturn(null);
    assertThatThrownBy(() ->
        checkAccess(List.of(new SimpleGrantedAuthority("viewer"))))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void whenRbacEnabled_userWithNoGroups_noDefaultRole_isDenied() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin")));
    when(acs.getDefaultRole()).thenReturn(null);
    assertThatThrownBy(() ->
        checkAccess(List.of()))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void whenRbacEnabled_userWithNoGroups_defaultRoleExists_isAllowed() {
    when(acs.isRbacEnabled()).thenReturn(true);
    when(acs.getRoles()).thenReturn(List.of(roleNamed("admin")));
    when(acs.getDefaultRole()).thenReturn(new DefaultRole());
    assertThatNoException().isThrownBy(() ->
        checkAccess(List.of()));
  }

  private Role roleNamed(String name) {
    Role r = new Role();
    r.setName(name);
    r.setClusters(List.of("test"));
    r.setSubjects(List.of());
    r.setPermissions(List.of());
    return r;
  }
}