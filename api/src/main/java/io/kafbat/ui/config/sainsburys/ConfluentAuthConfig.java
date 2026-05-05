package io.kafbat.ui.config.sainsburys;

import org.springframework.context.annotation.Configuration;
import java.util.Base64;

@Configuration
public class ConfluentAuthConfig {
  public static String generateBasicAuthentication(String access, String secret){
    byte[] encoded = Base64.getEncoder().encode(String.format("%s:%s", access, secret).getBytes());
    return String.format("Basic %s", new String(encoded));
  }
}
