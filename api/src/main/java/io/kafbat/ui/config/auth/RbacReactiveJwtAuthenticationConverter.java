package io.kafbat.ui.config.auth;

import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.convertRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.extractUsernameRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.matchRoles;

import com.google.common.collect.Sets;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RbacReactiveJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final AccessControlService accessControlService;
  @Nullable
  private final String rolesClaim;
  @Nullable
  private final String usernameClaim;

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    String username = usernameClaim != null && jwt.hasClaim(usernameClaim)
        ? jwt.getClaimAsString(usernameClaim)
        : jwt.getSubject();
    Collection<String> tokenRoles = rolesClaim != null && jwt.hasClaim(rolesClaim)
        ? convertRoles(jwt.getClaim(rolesClaim))
        : Set.of();

    Set<String> groups = Sets.union(
        extractUsernameRoles(accessControlService, username),
        matchRoles(accessControlService, tokenRoles));

    return Mono.just(new RbacBearerTokenAuthentication(
        new RbacBearerTokenUser(username, groups), jwt.getTokenValue()));
  }
}
