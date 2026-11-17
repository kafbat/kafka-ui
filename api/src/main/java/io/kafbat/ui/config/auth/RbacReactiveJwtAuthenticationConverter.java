package io.kafbat.ui.config.auth;

import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
    this.authoritiesClaimName =
        StringUtils.hasText(jwt.getAuthoritiesClaimName()) ? jwt.getAuthoritiesClaimName() : null;
    this.authoritiesClaimDelimiter =
        StringUtils.hasLength(jwt.getAuthoritiesClaimDelimiter()) ? jwt.getAuthoritiesClaimDelimiter() : " ";

    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(jwt.getAuthorityPrefix()).to(authoritiesConverter::setAuthorityPrefix);
    authoritiesConverter.setAuthoritiesClaimDelimiter(authoritiesClaimDelimiter);
    map.from(authoritiesClaimName).to(authoritiesConverter::setAuthoritiesClaimName);
    map.from(jwt.getPrincipalClaimName()).whenHasText().to(delegate::setPrincipalClaimName);
    delegate.setJwtGrantedAuthoritiesConverter(token -> authoritiesConverter.convert(withTextAuthorities(token)));
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    var token = delegate.convert(jwt);
    return Mono.just(RbacBearerTokenAuthentication.of(
        accessControlService, jwt.getTokenValue(), token.getName(), jwt.getSubject(), token.getAuthorities()));
  }

  private Jwt withTextAuthorities(Jwt jwt) {
    var claimName = authoritiesClaimName != null
        ? authoritiesClaimName
        : Stream.of("scope", "scp").filter(jwt::hasClaim).findFirst().orElse(null);
    if (claimName == null || !jwt.hasClaim(claimName)) {
      return jwt;
    }
    var claims = new HashMap<>(jwt.getClaims());
    claims.put(claimName, textAuthorities(jwt.getClaim(claimName)));
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
