package io.kafbat.ui.service.rbac.extractor;

import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

@Slf4j
public class RbacActiveDirectoryAuthoritiesExtractor implements LdapAuthoritiesPopulator {
  private final AccessControlService controlService;

  public RbacActiveDirectoryAuthoritiesExtractor(AccessControlService controlService) {
    this.controlService = controlService;
  }

  @Override
  public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
    String[] groups = userData.getStringAttributes("memberOf");

    if (log.isDebugEnabled() && groups != null && groups.length > 0) {
      log.debug("'memberOf' attribute values: {}", Arrays.asList(groups));
    }

    if (controlService != null) {
      return controlService.getRoles().stream()
          .filter(role -> role.getSubjects().stream()
              .filter(subject -> Provider.LDAP_AD.equals(subject.getProvider()))
              .anyMatch(subject -> switch (subject.getType()) {
                case "user" -> username.equals(subject.getValue());
                case "group" -> groups != null && Arrays.stream(groups)
                    .map(this::groupName)
                    .anyMatch(name -> name.equals(subject.getValue()));
                default -> false;
              })
          )
          .map(Role::getName)
          .peek(role -> log.trace("Mapped role [{}] for user [{}]", role, username))
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toList());
    } else {
      if (groups == null) {
        return AuthorityUtils.NO_AUTHORITIES;
      }

      List<GrantedAuthority> authorities = new ArrayList<>(groups.length);

      for (String group : groups) {
        authorities.add(new SimpleGrantedAuthority(groupName(group)));
      }

      return authorities;
    }
  }

  @SuppressWarnings("deprecation")
  private String groupName(String group) {
    return new DistinguishedName(group).removeLast().getValue();
  }
}
