package io.kafbat.ui.config.auth;

import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.RbacBasicAuthAuthoritiesExtractor;
import io.kafbat.ui.util.StaticFileWebFilter;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(value = "auth.type", havingValue = "LOGIN_FORM")
@EnableConfigurationProperties(SecurityProperties.class)
@Slf4j
public class BasicAuthSecurityConfig extends AbstractAuthSecurityConfig {
  private static final String NOOP_PASSWORD_PREFIX = "{noop}";
  private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

  @Bean
  public SecurityWebFilterChain configure(ServerHttpSecurity http) {
    log.info("Configuring LOGIN_FORM authentication.");

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

  @Bean
  public ReactiveUserDetailsService reactiveUserDetailsService(SecurityProperties properties,
                                                               ObjectProvider<PasswordEncoder> passwordEncoder,
                                                               AccessControlService accessControlService) {
    SecurityProperties.User user = properties.getUser();

    UserDetails userDetails = User.withUsername(user.getName())
        .password(password(user.getPassword(), passwordEncoder.getIfAvailable()))
        .roles(StringUtils.toStringArray(user.getRoles()))
        .build();

    if (accessControlService.isRbacEnabled()) {
      RbacBasicAuthAuthoritiesExtractor extractor = new RbacBasicAuthAuthoritiesExtractor(accessControlService);

      return new RbacUserDetailsService(new RbacBasicAuthUser(userDetails, extractor.groups(user.getName())));
    } else {
      return new MapReactiveUserDetailsService(userDetails);
    }
  }

  private String password(String password, PasswordEncoder encoder) {
    if (encoder != null || PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
      return password;
    }

    return NOOP_PASSWORD_PREFIX + password;
  }

  private record RbacUserDetailsService(RbacBasicAuthUser userDetails) implements ReactiveUserDetailsService {
    @Override
    public Mono<UserDetails> findByUsername(String username) {
      return (userDetails.getUsername().equals(username)) ? Mono.just(userDetails) : Mono.empty();
    }
  }

}
