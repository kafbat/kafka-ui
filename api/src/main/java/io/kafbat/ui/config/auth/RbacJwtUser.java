package io.kafbat.ui.config.auth;

import java.util.Collection;
import org.springframework.security.oauth2.jwt.Jwt;

public record RbacJwtUser(Jwt jwt, String username, Collection<String> groups) implements RbacUser {

  @Override
  public String name() {
    return username;
  }
}
