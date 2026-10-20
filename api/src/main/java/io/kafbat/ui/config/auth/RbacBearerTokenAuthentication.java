package io.kafbat.ui.config.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class RbacBearerTokenAuthentication extends AbstractAuthenticationToken {

  private final RbacBearerTokenUser principal;
  private final String token;

  public RbacBearerTokenAuthentication(RbacBearerTokenUser principal, String token) {
    super(principal.groups().stream().map(SimpleGrantedAuthority::new).toList());
    this.principal = principal;
    this.token = token;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public RbacBearerTokenUser getPrincipal() {
    return principal;
  }
}
