package io.kafbat.ui.config.auth;

import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.extractUsernameRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.matchRoles;

import com.google.common.collect.Sets;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

public class RbacReactiveJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final AccessControlService accessControlService;
  private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
  @Nullable
  private final String authoritiesClaimName;
  private final String authoritiesClaimDelimiter;

  public RbacReactiveJwtAuthenticationConverter(AccessControlService accessControlService,
                                                OAuth2ResourceServerProperties.Jwt jwt) {
    this.accessControlService = accessControlService;
    this.authoritiesClaimName = jwt.getAuthoritiesClaimName();
    this.authoritiesClaimDelimiter = Objects.requireNonNullElse(jwt.getAuthoritiesClaimDelimiter(), " ");

    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(jwt.getAuthorityPrefix()).to(authoritiesConverter::setAuthorityPrefix);
    map.from(jwt.getAuthoritiesClaimDelimiter()).to(authoritiesConverter::setAuthoritiesClaimDelimiter);
    map.from(jwt.getAuthoritiesClaimName()).to(authoritiesConverter::setAuthoritiesClaimName);
    map.from(jwt.getPrincipalClaimName()).to(delegate::setPrincipalClaimName);
    delegate.setJwtGrantedAuthoritiesConverter(token -> authoritiesConverter.convert(withTextAuthorities(token)));
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    var token = delegate.convert(jwt);
    return Mono.just(new RbacBearerTokenAuthentication(
        toUser(accessControlService, token.getName(), jwt.getSubject(), token.getAuthorities()),
        jwt.getTokenValue()));
  }

  static RbacBearerTokenUser toUser(AccessControlService accessControlService, @Nullable String username,
                                    String fallbackName, Collection<? extends GrantedAuthority> authorities) {
    var roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    Set<String> groups = Sets.union(
        username == null ? Set.of() : extractUsernameRoles(accessControlService, username),
        matchRoles(accessControlService, roles));
    return new RbacBearerTokenUser(username != null ? username : fallbackName, groups);
  }

  private Jwt withTextAuthorities(Jwt jwt) {
    if (authoritiesClaimName == null || !jwt.hasClaim(authoritiesClaimName)) {
      return jwt;
    }
    var claims = new HashMap<>(jwt.getClaims());
    claims.put(authoritiesClaimName, textAuthorities(jwt.getClaim(authoritiesClaimName)));
    return new Jwt(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), claims);
  }

  private Object textAuthorities(Object claim) {
    Collection<?> authorities;
    if (claim instanceof String value) {
      authorities = StringUtils.hasText(value) ? Arrays.asList(value.split(authoritiesClaimDelimiter)) : List.of();
    } else if (claim instanceof Collection<?> values) {
      authorities = values;
    } else {
      return claim;
    }
    return authorities.stream().filter(a -> a instanceof String role && StringUtils.hasText(role)).toList();
  }
}
