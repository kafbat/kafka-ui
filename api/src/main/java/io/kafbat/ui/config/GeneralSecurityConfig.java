package io.kafbat.ui.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.server.firewall.StrictServerWebExchangeFirewall;

@Configuration
public class GeneralSecurityConfig {

  @Bean
  public StrictServerWebExchangeFirewall strictServerWebExchangeFirewall() {
    StrictServerWebExchangeFirewall firewall = new StrictServerWebExchangeFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

}
