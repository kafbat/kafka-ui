package io.kafbat.ui.service.rbac.extractor;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.authentication.ad.DefaultActiveDirectoryAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

@Slf4j
public class RbacActiveDirectoryAuthoritiesExtractor implements LdapAuthoritiesPopulator {

  private final DefaultActiveDirectoryAuthoritiesPopulator populator = new DefaultActiveDirectoryAuthoritiesPopulator();
  private final AccessControlService acs;

  public RbacActiveDirectoryAuthoritiesExtractor(ApplicationContext context) {
    this.acs = context.getBean(AccessControlService.class);
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    var adGroups = populator.getGrantedAuthorities(userData, username)
        .stream()
        .map(GrantedAuthority::getAuthority)
        .peek(group -> log.trace("Found AD group [{}] for user [{}]", group, username))
        .collect(Collectors.toSet());

    return acs.getRoles()
        .stream()
        .filter(r -> r.getSubjects()
            .stream()
            .filter(subject -> subject.getProvider().equals(Provider.LDAP_AD))
            .anyMatch(subject -> switch (subject.getType()) {
              case "user" -> subject.matches(username);
              case "group" ->  adGroups.stream().anyMatch(subject::matches);
              default -> false;
            })
        )
        .map(Role::getName)
        .peek(role -> log.trace("Mapped role [{}] for user [{}]", role, username))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }
}
