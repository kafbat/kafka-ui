package io.kafbat.ui.config.auth;

import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.RbacActiveDirectoryAuthoritiesExtractor;
import io.kafbat.ui.service.rbac.extractor.RbacLdapAuthoritiesExtractor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.DefaultActiveDirectoryAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(value = "auth.type", havingValue = "LDAP")
@EnableConfigurationProperties(LdapProperties.class)
@RequiredArgsConstructor
@Slf4j
public class LdapSecurityConfig extends AbstractAuthSecurityConfig {

  private final LdapProperties props;

  @Bean
  public ReactiveAuthenticationManager authenticationManager(AbstractLdapAuthenticationProvider authProvider) {
    return new ReactiveAuthenticationManagerAdapter(new ProviderManager(List.of(authProvider)));
  }

  @Bean
  public AbstractLdapAuthenticationProvider authenticationProvider(LdapAuthoritiesPopulator authoritiesExtractor,
                                                                   BindAuthenticator bindAuthenticator,
                                                                   AccessControlService acs) {
    var rbacEnabled = acs.isRbacEnabled();

    AbstractLdapAuthenticationProvider authProvider;

    if (!props.isActiveDirectory()) {
      authProvider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesExtractor);
    } else {
      authProvider = new ActiveDirectoryLdapAuthenticationProvider(props.getActiveDirectoryDomain(),
          props.getUrls());
      authProvider.setUseAuthenticationRequestCredentials(true);
      ((ActiveDirectoryLdapAuthenticationProvider) authProvider).setAuthoritiesPopulator(authoritiesExtractor);
    }

    if (rbacEnabled) {
      authProvider.setUserDetailsContextMapper(new RbacUserDetailsMapper());
    }

    return authProvider;
  }

  @Bean
  public BindAuthenticator ldapBindAuthentication(LdapContextSource ldapContextSource) {
    BindAuthenticator ba = new BindAuthenticator(ldapContextSource);

    if (props.getBase() != null) {
      ba.setUserDnPatterns(new String[] {props.getBase()});
    }

    if (props.getUserFilterSearchFilter() != null) {
      LdapUserSearch userSearch =
          new FilterBasedLdapUserSearch(props.getUserFilterSearchBase(), props.getUserFilterSearchFilter(),
              ldapContextSource);
      ba.setUserSearch(userSearch);
    }

    return ba;
  }

  @Bean
  public LdapContextSource ldapContextSource() {
    LdapContextSource ctx = new LdapContextSource();
    ctx.setUrl(props.getUrls());
    ctx.setUserDn(props.getAdminUser());
    ctx.setPassword(props.getAdminPassword());
    ctx.afterPropertiesSet();
    return ctx;
  }

  @Bean
  public LdapAuthoritiesPopulator authoritiesExtractor(ApplicationContext ctx,
                                                       BaseLdapPathContextSource ldapCtx,
                                                       AccessControlService acs) {
    if (!props.isActiveDirectory()) {
      if (!acs.isRbacEnabled()) {
        return new NullLdapAuthoritiesPopulator();
      }

      var extractor = new RbacLdapAuthoritiesExtractor(ctx, ldapCtx, props.getGroupFilterSearchBase());

      Optional.ofNullable(props.getGroupFilterSearchFilter()).ifPresent(extractor::setGroupSearchFilter);
      extractor.setRolePrefix("");
      extractor.setConvertToUpperCase(false);
      extractor.setSearchSubtree(true);

      return extractor;
    } else {
      return acs.isRbacEnabled()
          ? new RbacActiveDirectoryAuthoritiesExtractor(ctx)
          : new DefaultActiveDirectoryAuthoritiesPopulator();
    }
  }

  @Bean
  public SecurityWebFilterChain configureLdap(ServerHttpSecurity http) {
    log.info("Configuring LDAP authentication.");
    if (props.isActiveDirectory()) {
      log.info("Active Directory support for LDAP has been enabled.");
    }

    return http.authorizeExchange(spec -> spec
            .pathMatchers(AUTH_WHITELIST)
            .permitAll()
            .anyExchange()
            .authenticated()
        )
        .formLogin(Customizer.withDefaults())
        .logout(Customizer.withDefaults())
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
  }

  private static class RbacUserDetailsMapper extends LdapUserDetailsMapper {
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
      UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
      return new RbacLdapUser(userDetails);
    }
  }

}

