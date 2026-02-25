package io.kafbat.ui.config.auth;

import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.StaticFileWebFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
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
      ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient,
      ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService,
      ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService,
      @Qualifier("oauthWebClient") WebClient webClient
  ) {
    log.info("Configuring OAUTH2 authentication.");

    var oidcAuthManager =
        new OidcAuthorizationCodeReactiveAuthenticationManager(tokenResponseClient, oidcUserService);

    oidcAuthManager.setJwtDecoderFactory(clientRegistration ->
        NimbusReactiveJwtDecoder.withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
            .webClient(webClient)
            .build());

    var oauth2AuthManager =
        new OAuth2LoginReactiveAuthenticationManager(tokenResponseClient, oauth2UserService);

    var delegatingAuthManager =
        new DelegatingReactiveAuthenticationManager(oidcAuthManager, oauth2AuthManager);

    var builder = http.authorizeExchange(spec -> spec
            .pathMatchers(AUTH_WHITELIST)
            .permitAll()
            .anyExchange()
            .authenticated()
        )
        .oauth2Login(oauth2 -> oauth2.authenticationManager(delegatingAuthManager))
        .logout(spec -> spec.logoutSuccessHandler(logoutHandler))
        .csrf(ServerHttpSecurity.CsrfSpec::disable);

    if (properties.getResourceServer() != null) {
      OAuth2ResourceServerProperties resourceServer = properties.getResourceServer();
      if (resourceServer.getJwt() != null && resourceServer.getJwt().getJwkSetUri() != null) {
        builder.oauth2ResourceServer(c -> c.jwt(j ->
            j.jwtDecoder(NimbusReactiveJwtDecoder.withJwkSetUri(resourceServer.getJwt().getJwkSetUri())
                .webClient(webClient)
                .build())));
      } else if (resourceServer.getOpaquetoken() != null
          && resourceServer.getOpaquetoken().getIntrospectionUri() != null) {
        OAuth2ResourceServerProperties.Opaquetoken opaquetoken = resourceServer.getOpaquetoken();
        builder.oauth2ResourceServer(c -> c.opaqueToken(o ->
            o.introspector(new SpringReactiveOpaqueTokenIntrospector(
                opaquetoken.getIntrospectionUri(),
                webClient.mutate()
                    .defaultHeaders(h -> h.setBasicAuth(opaquetoken.getClientId(), opaquetoken.getClientSecret()))
                    .build()))));
      }
    }

    builder.addFilterAt(new StaticFileWebFilter(), SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING);

    return builder.build();
  }

  @Bean
  public ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      authorizationCodeTokenResponseClient(@Qualifier("oauthWebClient") WebClient webClient) {
    var client = new WebClientReactiveAuthorizationCodeTokenResponseClient();
    client.setWebClient(webClient);
    return client;
  }

  @Bean
  public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> customOidcUserService(
      AccessControlService acs,
      ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService) {
    final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

    delegate.setOauth2UserService(oauth2UserService);

    return request -> delegate.loadUser(request)
        .flatMap(user -> {
          var provider = getProviderByProviderId(request.getClientRegistration().getRegistrationId());
          final var extractor = getExtractor(provider, acs);
          if (extractor == null) {
            return Mono.just(user);
          }

          return extractor.extract(acs, user, Map.of("request", request, "provider", provider))
              .map(groups -> new RbacOidcUser(user, groups));
        });
  }

  @Bean
  public ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> customOauth2UserService(
      AccessControlService acs, @Qualifier("oauthWebClient") WebClient webClient) {
    final DefaultReactiveOAuth2UserService delegate = new DefaultReactiveOAuth2UserService();
    delegate.setWebClient(webClient);

    return request -> delegate.loadUser(request)
        .flatMap(user -> {
          var provider = getProviderByProviderId(request.getClientRegistration().getRegistrationId());
          final var extractor = getExtractor(provider, acs);
          if (extractor == null) {
            return Mono.just(user);
          }

          return extractor.extract(acs, user, Map.of("request", request, "provider", provider))
              .map(groups -> new RbacOAuth2User(user, groups));
        });
  }

  @Bean
  public InMemoryReactiveClientRegistrationRepository clientRegistrationRepository() {
    final OAuth2ClientProperties props = OAuthPropertiesConverter.convertProperties(properties);
    final List<ClientRegistration> registrations =
        new ArrayList<>(new OAuth2ClientPropertiesMapper(props).asClientRegistrations().values());
    if (registrations.isEmpty()) {
      throw new IllegalArgumentException("OAuth2 authentication is enabled but no providers specified.");
    }
    return new InMemoryReactiveClientRegistrationRepository(registrations);
  }

  @Bean
  public ServerLogoutSuccessHandler defaultOidcLogoutHandler(final ReactiveClientRegistrationRepository repository) {
    return new OidcClientInitiatedServerLogoutSuccessHandler(repository);
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

