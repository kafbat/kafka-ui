package io.kafbat.ui.config.auth;

import io.kafbat.ui.service.rbac.AccessControlService;
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

  public RbacReactiveJwtAuthenticationConverter(AccessControlService accessControlService,
                                                OAuth2ResourceServerProperties.Jwt jwt) {
    this.accessControlService = accessControlService;

    var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    var map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(jwt.getAuthorityPrefix()).to(authoritiesConverter::setAuthorityPrefix);
    map.from(jwt.getAuthoritiesClaimDelimiter()).when(StringUtils::hasLength)
        .to(authoritiesConverter::setAuthoritiesClaimDelimiter);
    map.from(jwt.getAuthoritiesClaimName()).whenHasText().to(authoritiesConverter::setAuthoritiesClaimName);
    map.from(jwt.getPrincipalClaimName()).whenHasText().to(delegate::setPrincipalClaimName);
    delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
  }

  @Override
  public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
    var token = delegate.convert(jwt);
    return Mono.just(RbacBearerTokenAuthentication.of(
        accessControlService, jwt.getTokenValue(), token.getName(), jwt.getSubject(), token.getAuthorities()));
  }
}
