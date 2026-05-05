package io.kafbat.ui.config.sainsburys;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class HttpFeignConfig {
  @Bean
  public HttpMessageConverters messageConverters() {
    return new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
  }
}
