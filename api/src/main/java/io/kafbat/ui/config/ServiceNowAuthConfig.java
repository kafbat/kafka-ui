package io.kafbat.ui.config;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceNowAuthConfig {
  @Value("${kit.external.services.service-now.authentication.api-key}")
  private String apiKey;

  @Value("${kit.external.services.service-now.authentication.api-secret}")
  private String apiSecret;

  @Bean
  public BasicAuthRequestInterceptor serviceNowBasicAuthRequestInterceptor(){
    return new BasicAuthRequestInterceptor(apiKey, apiSecret);
  }
}
