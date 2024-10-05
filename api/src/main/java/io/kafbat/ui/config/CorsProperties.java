package io.kafbat.ui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "cors")
@Data

public class CorsProperties {

  private String allowedOrigins;
  private String allowedMethods;
  private String allowedHeaders;
  private String maxAge;

}
