package io.kafbat.ui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {

  private String allowedOrigins;
  private String allowedMethods;
  private String allowedHeaders;
  private String allowCredentials;
  private String maxAge;

}
