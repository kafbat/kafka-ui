package io.kafbat.ui.config.auth;

import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.convertRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.extractUsernameRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.matchRoles;

import com.google.common.collect.Sets;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
    return Mono.just(new RbacBearerTokenAuthentication(toUser(jwt.getSubject(), jwt::getClaim), jwt.getTokenValue()));
  }

  RbacBearerTokenUser toUser(String subject, Function<String, Object> claims) {
    String username = usernameClaim == null ? subject : Objects.toString(claims.apply(usernameClaim), null);
    Object rolesValue = rolesClaim == null ? null : claims.apply(rolesClaim);
    Collection<String> tokenRoles = rolesValue == null ? Set.of() : convertRoles(rolesValue);

    Set<String> groups = Sets.union(
        username == null ? Set.of() : extractUsernameRoles(accessControlService, username),
        matchRoles(accessControlService, tokenRoles));

    return new RbacBearerTokenUser(username != null ? username : subject, groups);
  }
}
