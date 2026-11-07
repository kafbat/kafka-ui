package io.kafbat.ui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class CodecConfig {

  // runs after Spring Boot's Jackson codec customizer (order 0), which sets the default encoder
  @Bean
  @Order(1)
  public CodecCustomizer stringJsonEncoderCustomizer(ObjectMapper objectMapper) {
    return configurer -> configurer.defaultCodecs().jackson2JsonEncoder(new StringJackson2JsonEncoder(objectMapper));
  }
}
