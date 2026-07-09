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

  @Bean(name = "oauthWebClient")
  public WebClient oauthWebClient() {
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create().proxyWithSystemProperties()))
        .build();
  }

  @Bean
  public SecurityWebFilterChain configure(
      ServerHttpSecurity http,
      Optional<OAuthLogoutSuccessHandler> logoutHandler,
      Optional<ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>> tokenResponseClient,
      Optional<ReactiveOAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService,
      Optional<ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User>> oauth2UserService,
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

    if (tokenResponseClient.isPresent() && oidcUserService.isPresent() && oauth2UserService.isPresent()) {
      log.info("OAuth2 client registrations found, enabling interactive login.");

      var oidcAuthManager = new OidcAuthorizationCodeReactiveAuthenticationManager(
          tokenResponseClient.get(), oidcUserService.get());

      oidcAuthManager.setJwtDecoderFactory(clientRegistration ->
          NimbusReactiveJwtDecoder.withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
              .webClient(webClient)
              .build());

      var oauth2AuthManager = new OAuth2LoginReactiveAuthenticationManager(
          tokenResponseClient.get(), oauth2UserService.get());

      var delegatingAuthManager =
          new DelegatingReactiveAuthenticationManager(oidcAuthManager, oauth2AuthManager);

      builder.oauth2Login(oauth2 -> oauth2.authenticationManager(delegatingAuthManager));
      logoutHandler.ifPresent(handler -> builder.logout(spec -> spec.logoutSuccessHandler(handler)));
    } else {
      log.info("No OAuth2 client registrations, running in resource-server-only mode.");
    }

    if (properties.getResourceServer() != null) {
      OAuth2ResourceServerProperties resourceServer = properties.getResourceServer();
      if (resourceServer.getJwt() != null && resourceServer.getJwt().getJwkSetUri() != null) {
        var jwtDecoder = NimbusReactiveJwtDecoder
            .withJwkSetUri(resourceServer.getJwt().getJwkSetUri())
            .webClient(webClient)
            .build();
        var rbacProps = properties.getResourceServerRbac();

        builder.oauth2ResourceServer(c -> c.jwt(j -> {
          j.jwtDecoder(jwtDecoder);
          if (rbacProps != null) {
            j.jwtAuthenticationConverter(new RbacReactiveJwtAuthenticationConverter(
                accessControlService,
                rbacProps.getRolesClaim(),
                rbacProps.getUsernameClaim(),
                rbacProps.getEntityTypeClaim(),
                rbacProps.getDefaultRole()));
          }
        }));
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
      Optional<ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User>> oauth2UserService) {
    if (!hasClientRegistrations() || oauth2UserService.isEmpty()) {
      return null;
    }
    final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();
    delegate.setOauth2UserService(oauth2UserService.get());

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

          return extractor.extract(acs, user, Map.of("request", request, "provider", provider))
              .map(groups -> new RbacOAuth2User(user, groups));
        });
  }

  @Bean
  public ReactiveClientRegistrationRepository clientRegistrationRepository() {
    if (!hasClientRegistrations()) {
      return registrationId -> Mono.empty();
    }
    final OAuth2ClientProperties props = OAuthPropertiesConverter.convertProperties(properties);
    final List<ClientRegistration> registrations =
        new ArrayList<>(new OAuth2ClientPropertiesMapper(props).asClientRegistrations().values());
    return new InMemoryReactiveClientRegistrationRepository(registrations);
  }

  @Bean
  public ServerLogoutSuccessHandler defaultOidcLogoutHandler(
      ReactiveClientRegistrationRepository repository) {
    if (!hasClientRegistrations()) {
      return null;
    }
    return new OidcClientInitiatedServerLogoutSuccessHandler(repository);
  }

  private boolean hasClientRegistrations() {
    return properties.getClient() != null && !properties.getClient().isEmpty();
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
