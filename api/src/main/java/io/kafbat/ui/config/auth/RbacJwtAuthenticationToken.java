package io.kafbat.ui.config.auth;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class RbacJwtAuthenticationToken extends AbstractAuthenticationToken {

  private final RbacJwtUser principal;
  private final Jwt jwt;

  public RbacJwtAuthenticationToken(RbacJwtUser principal, Jwt jwt,
                                    Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.jwt = jwt;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return jwt.getTokenValue();
  }

  @Override
  public RbacJwtUser getPrincipal() {
    return principal;
  }
}
