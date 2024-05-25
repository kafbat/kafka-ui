package io.kafbat.ui.service.rbac.extractor;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.NestedLdapAuthoritiesPopulator;

@Slf4j
public class RbacLdapAuthoritiesExtractor extends NestedLdapAuthoritiesPopulator {

  private final AccessControlService acs;

  public RbacLdapAuthoritiesExtractor(ApplicationContext context,
                                      BaseLdapPathContextSource contextSource, String groupFilterSearchBase) {
    super(contextSource, groupFilterSearchBase);
    this.acs = context.getBean(AccessControlService.class);
  }

  @Override
  protected Set<GrantedAuthority> getAdditionalRoles(DirContextOperations user, String username) {
    var ldapGroups = super.getGroupMembershipRoles(user.getNameInNamespace(), username)
        .stream()
        .map(GrantedAuthority::getAuthority)
        .peek(group -> log.trace("Found LDAP group [{}] for user [{}]", group, username))
        .collect(Collectors.toSet());

    return acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(subject -> subject.getProvider().equals(Provider.LDAP))
            .filter(subject -> subject.getType().equals("group"))
            .anyMatch(subject -> ldapGroups.contains(subject.getValue()))
        )
        .map(Role::getName)
        .peek(role -> log.trace("Mapped role [{}] for user [{}]", role, username))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }
}
