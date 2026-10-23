package io.kafbat.ui.config.auth;

import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.extractUsernameRoles;
import static io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor.matchRoles;

import com.google.common.collect.Sets;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collection;
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
import reactor.core.publisher.Mono;

public class RbacReactiveJwtAuthenticationConverter
    implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

  private final AccessControlService accessControlService;
  private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

  public RbacReactiveJwtAuthenticationConverter(AccessControlService accessControlService,
                                                OAuth2ResourceServerProperties.Jwt jwt) {
    this.accessControlService = accessControlService;

    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(jwt.getAuthorityPrefix()).to(authoritiesConverter::setAuthorityPrefix);
    map.from(jwt.getAuthoritiesClaimDelimiter()).to(authoritiesConverter::setAuthoritiesClaimDelimiter);
    map.from(jwt.getAuthoritiesClaimName()).to(authoritiesConverter::setAuthoritiesClaimName);
    map.from(jwt.getPrincipalClaimName()).to(delegate::setPrincipalClaimName);
    delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
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
}
