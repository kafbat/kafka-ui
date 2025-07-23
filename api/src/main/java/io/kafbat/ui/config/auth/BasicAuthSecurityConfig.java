package io.kafbat.ui.config.auth;

import io.kafbat.ui.util.StaticFileWebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(value = "auth.type", havingValue = "LOGIN_FORM")
@Slf4j
public class BasicAuthSecurityConfig extends AbstractAuthSecurityConfig {

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

}
