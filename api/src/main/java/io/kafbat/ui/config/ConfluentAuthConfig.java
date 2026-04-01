package io.kafbat.ui.config;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfluentAuthConfig {
  @Value("${kit.external.services.confluent-api.authentication.api-key}")
  private String apiKey;

  @Value("${kit.external.services.confluent-api.authentication.api-secret}")
  private String apiSecret;

  @Bean
  public BasicAuthRequestInterceptor confluentCloudApiBasicAuthRequestInterceptor(){
    return new BasicAuthRequestInterceptor(apiKey, apiSecret);
  }
}
