package io.kafbat.ui.config.auth;

import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.StaticFileWebFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@ConditionalOnProperty(value = "auth.type", havingValue = "OAUTH2")
@EnableConfigurationProperties(OAuthProperties.class)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class OAuthSecurityConfig extends AbstractAuthSecurityConfig {

  private final OAuthProperties properties;

  /**
   * WebClient configured to use system proxy properties (http.proxyHost/https.proxyHost,
   * http.proxyPort/https.proxyPort, http.nonProxyHosts/https.nonProxyHosts).
   * Created as a bean to ensure system properties are read after context initialization.
   */
  @Bean(name = "oauthWebClient")
  public WebClient oauthWebClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create().proxyWithSystemProperties()))
        .build();
  }

  @Bean
  public SecurityWebFilterChain configure(
      ServerHttpSecurity http,
      OAuthLogoutSuccessHandler logoutHandler,
      @Nullable ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient,
      @Nullable ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
      @Nullable ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
      @Qualifier("oauthWebClient") WebClient webClient,
      AccessControlService accessControlService
  ) {
    log.info("Configuring OAUTH2 authentication.");

    var builder = http.authorizeExchange(spec -> spec
            .pathMatchers(AUTH_WHITELIST)
            .permitAll()
            .anyExchange()
            .authenticated()
        )
        .csrf(ServerHttpSecurity.CsrfSpec::disable);

    if (hasClientRegistrations()) {
      log.info("OAuth2 client registrations found, enabling interactive login.");

      var oidcAuthManager = new OidcAuthorizationCodeReactiveAuthenticationManager(
          tokenResponseClient, oidcUserService);

      oidcAuthManager.setJwtDecoderFactory(clientRegistration ->
          NimbusReactiveJwtDecoder.withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
              .webClient(webClient)
              .build());

      var oauth2AuthManager = new OAuth2LoginReactiveAuthenticationManager(
          tokenResponseClient, oauth2UserService);

      var delegatingAuthManager =
          new DelegatingReactiveAuthenticationManager(oidcAuthManager, oauth2AuthManager);

      builder.oauth2Login(oauth2 -> oauth2.authenticationManager(delegatingAuthManager));
      builder.logout(spec -> spec.logoutSuccessHandler(logoutHandler));
    } else {
      log.info("No OAuth2 client registrations, running in resource-server-only mode.");
    }

    OAuth2ResourceServerProperties resourceServer = properties.getResourceServer();
    if (hasJwtResourceServer()) {
      var jwtDecoder = jwtDecoder(resourceServer.getJwt(), webClient);
      builder.oauth2ResourceServer(c -> c.jwt(j -> {
        j.jwtDecoder(jwtDecoder);
        if (accessControlService.isRbacEnabled()) {
          j.jwtAuthenticationConverter(rbacConverter(accessControlService));
        }
      }));
    } else if (hasOpaqueTokenResourceServer()) {
      OAuth2ResourceServerProperties.Opaquetoken opaquetoken = resourceServer.getOpaquetoken();
      builder.oauth2ResourceServer(c -> c.opaqueToken(o -> {
        o.introspector(new SpringReactiveOpaqueTokenIntrospector(
            opaquetoken.getIntrospectionUri(),
            webClient.mutate()
                .defaultHeaders(h -> h.setBasicAuth(opaquetoken.getClientId(), opaquetoken.getClientSecret()))
                .build()));
        if (accessControlService.isRbacEnabled()) {
          var converter = rbacConverter(accessControlService);
          o.authenticationConverter((token, principal) -> Mono.just(new RbacBearerTokenAuthentication(
              converter.toUser(principal.getName(), principal::getAttribute), token)));
        }
      }));
    }

    builder.addFilterAt(new StaticFileWebFilter(), SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING);

    return builder.build();
  }

  @Bean
  public ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      authorizationCodeTokenResponseClient(@Qualifier("oauthWebClient") WebClient webClient) {
    if (!hasClientRegistrations()) {
      return null;
    }
    var client = new WebClientReactiveAuthorizationCodeTokenResponseClient();
    client.setWebClient(webClient);
    return client;
  }

  @Bean
  public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService(
      AccessControlService acs,
      @Nullable ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
      @Qualifier("oauthWebClient") WebClient webClient) {
    if (!hasClientRegistrations()) {
      return null;
    }
    final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

    delegate.setOauth2UserService(oauth2UserService);

    return request -> delegate.loadUser(request)
        .flatMap(user -> {
          var provider = getProviderByProviderId(request.getClientRegistration().getRegistrationId());
          final var extractor = getExtractor(provider, acs);
          if (extractor == null) {
            return Mono.just(user);
          }

          return extractor.extract(acs, user, Map.of(
                  "request", request,
                  "provider", provider,
                  ProviderAuthorityExtractor.OAUTH_WEB_CLIENT, webClient))
              .map(groups -> new RbacOidcUser(user, groups));
        });
  }

  @Bean
  public ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> customOauth2UserService(
      AccessControlService acs, @Qualifier("oauthWebClient") WebClient webClient) {
    if (!hasClientRegistrations()) {
      return null;
    }
    final DefaultReactiveOAuth2UserService delegate = new DefaultReactiveOAuth2UserService();
    delegate.setWebClient(webClient);

    return request -> delegate.loadUser(request)
        .flatMap(user -> {
          var provider = getProviderByProviderId(request.getClientRegistration().getRegistrationId());
          final var extractor = getExtractor(provider, acs);
          if (extractor == null) {
            return Mono.just(user);
          }

          return extractor.extract(acs, user, Map.of(
                  "request", request,
                  "provider", provider,
                  ProviderAuthorityExtractor.OAUTH_WEB_CLIENT, webClient))
              .map(groups -> new RbacOAuth2User(user, groups));
        });
  }

  @Bean
  public ReactiveClientRegistrationRepository clientRegistrationRepository() {
    if (!hasClientRegistrations()) {
      if (!hasJwtResourceServer() && !hasOpaqueTokenResourceServer()) {
        throw new IllegalArgumentException("OAuth2 authentication is enabled but no providers specified.");
      }
      return registrationId -> Mono.empty();
    }
    final OAuth2ClientProperties props = OAuthPropertiesConverter.convertProperties(properties);
    final List<ClientRegistration> registrations =
        new ArrayList<>(new OAuth2ClientPropertiesMapper(props).asClientRegistrations().values());
    return new InMemoryReactiveClientRegistrationRepository(registrations);
  }

  @Bean
  public ServerLogoutSuccessHandler defaultOidcLogoutHandler(final ReactiveClientRegistrationRepository repository) {
    if (!hasClientRegistrations()) {
      return null;
    }
    return new OidcClientInitiatedServerLogoutSuccessHandler(repository);
  }

  private RbacReactiveJwtAuthenticationConverter rbacConverter(AccessControlService accessControlService) {
    var rbacProps = Optional.ofNullable(properties.getResourceServerRbac())
        .orElseGet(OAuthProperties.ResourceServerRbac::new);
    return new RbacReactiveJwtAuthenticationConverter(
        accessControlService, rbacProps.getRolesClaim(), rbacProps.getUsernameClaim());
  }

  private static NimbusReactiveJwtDecoder jwtDecoder(OAuth2ResourceServerProperties.Jwt jwt, WebClient webClient) {
    var decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwt.getJwkSetUri())
        .webClient(webClient)
        .jwsAlgorithms(algorithms -> jwt.getJwsAlgorithms().stream()
            .map(SignatureAlgorithm::from)
            .forEach(algorithms::add))
        .build();

    List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
    validators.add(jwt.getIssuerUri() != null
        ? JwtValidators.createDefaultWithIssuer(jwt.getIssuerUri())
        : JwtValidators.createDefault());
    if (!CollectionUtils.isEmpty(jwt.getAudiences())) {
      validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
          aud -> aud != null && !Collections.disjoint(aud, jwt.getAudiences())));
    }
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
    return decoder;
  }

  private boolean hasClientRegistrations() {
    return properties.getClient() != null && !properties.getClient().isEmpty();
  }

  private boolean hasJwtResourceServer() {
    var resourceServer = properties.getResourceServer();
    return resourceServer != null
        && resourceServer.getJwt() != null
        && resourceServer.getJwt().getJwkSetUri() != null;
  }

  private boolean hasOpaqueTokenResourceServer() {
    var resourceServer = properties.getResourceServer();
    return resourceServer != null
        && resourceServer.getOpaquetoken() != null
        && resourceServer.getOpaquetoken().getIntrospectionUri() != null;
  }

  private ProviderAuthorityExtractor getExtractor(final OAuthProperties.OAuth2Provider provider,
                                                  AccessControlService acs) {
    Optional<ProviderAuthorityExtractor> extractor = acs.getOauthExtractors()
        .stream()
        .filter(e -> e.isApplicable(provider.getProvider(), provider.getCustomParams()))
        .findFirst();

    return extractor.orElse(null);
  }

  private OAuthProperties.OAuth2Provider getProviderByProviderId(final String providerId) {
    return properties.getClient().get(providerId);
  }

}
