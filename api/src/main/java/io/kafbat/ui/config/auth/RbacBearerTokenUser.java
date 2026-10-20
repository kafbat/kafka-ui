package io.kafbat.ui.config.auth;

import java.util.Collection;
import org.springframework.security.core.AuthenticatedPrincipal;

public record RbacBearerTokenUser(String name, Collection<String> groups)
    implements RbacUser, AuthenticatedPrincipal {

  @Override
  public String getName() {
    return name;
  }
}
