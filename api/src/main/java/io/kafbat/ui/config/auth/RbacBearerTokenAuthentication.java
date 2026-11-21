package io.kafbat.ui.config.auth;

import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.extractUsernameRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.matchRoles;

import com.google.common.collect.Sets;
import io.kafbat.ui.service.rbac.AccessControlService;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

public class RbacBearerTokenAuthentication extends AbstractAuthenticationToken {

  private final RbacBearerTokenUser principal;
  private final String token;

  private RbacBearerTokenAuthentication(RbacBearerTokenUser principal, String token,
                                        Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.token = token;
    setAuthenticated(true);
  }

  static RbacBearerTokenAuthentication of(AccessControlService accessControlService, String token,
                                          OAuth2AuthenticatedPrincipal principal) {
    return of(accessControlService, token, principal.getName(), principal.getName(), principal.getAuthorities());
  }

  static RbacBearerTokenAuthentication of(AccessControlService accessControlService, String token,
                                          @Nullable String username, String fallbackName,
                                          Collection<? extends GrantedAuthority> authorities) {
    var roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    Set<String> groups = Sets.union(
        extractUsernameRoles(accessControlService, username),
        matchRoles(accessControlService, roles));
    var user = new RbacBearerTokenUser(username != null ? username : fallbackName, groups);
    return new RbacBearerTokenAuthentication(user, token, authorities);
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
