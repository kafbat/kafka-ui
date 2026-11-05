package io.kafbat.ui.config.auth;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class RbacBearerTokenAuthentication extends AbstractAuthenticationToken {

  private final RbacBearerTokenUser principal;
  private final String token;

  public RbacBearerTokenAuthentication(RbacBearerTokenUser principal, String token,
                                       Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
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
