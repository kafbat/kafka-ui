package io.kafbat.ui.config.auth;

import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.RbacLdapAuthoritiesExtractor;
import io.kafbat.ui.util.StaticFileWebFilter;
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
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(value = "auth.type", havingValue = "LDAP")
@EnableConfigurationProperties(LdapProperties.class)
@RequiredArgsConstructor
@Slf4j
public class LdapSecurityConfig extends AbstractAuthSecurityConfig {

  private final LdapProperties props;

  @Bean
  public ReactiveAuthenticationManager authenticationManager(LdapContextSource ldapContextSource,
                                                             LdapAuthoritiesPopulator authoritiesExtractor,
                                                             AccessControlService acs) {
    var rbacEnabled = acs.isRbacEnabled();
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

    AbstractLdapAuthenticationProvider authenticationProvider;
    if (!props.isActiveDirectory()) {
      authenticationProvider = rbacEnabled
          ? new LdapAuthenticationProvider(ba, authoritiesExtractor)
          : new LdapAuthenticationProvider(ba);
    } else {
      authenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(props.getActiveDirectoryDomain(),
          props.getUrls()); // TODO Issue #3741
      authenticationProvider.setUseAuthenticationRequestCredentials(true);
    }

    if (rbacEnabled) {
      authenticationProvider.setUserDetailsContextMapper(new UserDetailsMapper());
    }

    AuthenticationManager am = new ProviderManager(List.of(authenticationProvider));

    return new ReactiveAuthenticationManagerAdapter(am);
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
  public DefaultLdapAuthoritiesPopulator ldapAuthoritiesExtractor(ApplicationContext context,
                                                                  BaseLdapPathContextSource contextSource,
                                                                  AccessControlService acs) {
    var rbacEnabled = acs != null && acs.isRbacEnabled();

    DefaultLdapAuthoritiesPopulator extractor;

    if (rbacEnabled) {
      extractor = new RbacLdapAuthoritiesExtractor(context, contextSource, props.getGroupFilterSearchBase());
    } else {
      extractor = new DefaultLdapAuthoritiesPopulator(contextSource, props.getGroupFilterSearchBase());
    }

    Optional.ofNullable(props.getGroupFilterSearchFilter()).ifPresent(extractor::setGroupSearchFilter);
    extractor.setRolePrefix("");
    extractor.setConvertToUpperCase(false);
    extractor.setSearchSubtree(true);
    return extractor;
  }

  @Bean
  public SecurityWebFilterChain configureLdap(ServerHttpSecurity http) {
    log.info("Configuring LDAP authentication.");
    if (props.isActiveDirectory()) {
      log.info("Active Directory support for LDAP has been enabled.");
    }

    var builder = http.authorizeExchange(spec -> spec
            .pathMatchers(AUTH_WHITELIST)
            .permitAll()
            .anyExchange()
            .authenticated()
        )
        .formLogin(form -> form
            .loginPage(LOGIN_URL)
            .authenticationSuccessHandler(emptyRedirectSuccessHandler())
        )
        .logout(spec -> spec
            .logoutSuccessHandler(redirectLogoutSuccessHandler())
            .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout")))
        .csrf(ServerHttpSecurity.CsrfSpec::disable);

    builder.addFilterAt(new StaticFileWebFilter(), SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING);

    return builder.build();
  }

  private static class UserDetailsMapper extends LdapUserDetailsMapper {
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
      UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
      return new RbacLdapUser(userDetails);
    }
  }

}

