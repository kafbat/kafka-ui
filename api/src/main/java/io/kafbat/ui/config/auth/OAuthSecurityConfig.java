package io.kafbat.ui.config.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.StaticFileWebFilter;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.util.StringUtils;
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

  private static final Duration OIDC_DISCOVERY_TIMEOUT = Duration.ofSeconds(15);
  private final OAuthProperties properties;

  /**
   * WebClient configured to use system proxy properties (http.proxyHost/https.proxyHost,
   * http.proxyPort/https.proxyPort, http.nonProxyHosts/https.nonProxyHosts) and optionally
   * skip TLS certificate verification when auth.oauth2.insecure-ssl=true.
   * Created as a bean to ensure system properties are read after context initialization.
   */
  @Bean(name = "oauthWebClient")
  public WebClient oauthWebClient() {
    HttpClient httpClient = HttpClient.create().proxyWithSystemProperties();
    if (properties.isInsecureSsl()) {
      try {
        var context = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        httpClient = httpClient.secure(t -> t.sslContext(context));
      } catch (Exception e) {
        throw new IllegalStateException("Failed to initialize OAuth insecure SSL context", e);
      }
    }
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
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
  public InMemoryReactiveClientRegistrationRepository clientRegistrationRepository(
      @Qualifier("oauthWebClient") WebClient webClient
  ) {
    Map<String, OidcDiscoveryResponse> discoveredOidcMetadata = enrichOidcMetadataIfInsecureSsl(webClient);
    final OAuth2ClientProperties props = OAuthPropertiesConverter.convertProperties(properties);
    final Map<String, ClientRegistration> mappedRegistrations =
       new OAuth2ClientPropertiesMapper(props).asClientRegistrations();
    final List<ClientRegistration> registrations = new ArrayList<>(mappedRegistrations.size());
    mappedRegistrations.forEach((providerId, registration) ->
        registrations.add(enrichClientRegistrationWithOidcMetadata(registration, discoveredOidcMetadata.get(providerId))));
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

  private Map<String, OidcDiscoveryResponse> enrichOidcMetadataIfInsecureSsl(WebClient webClient) {
    Map<String, OidcDiscoveryResponse> discoveredMetadata = new HashMap<>();
    if (!properties.isInsecureSsl()) {
      return discoveredMetadata;
    }

    properties.getClient().forEach((providerId, provider) ->
        enrichProviderOidcMetadata(webClient, discoveredMetadata, providerId, provider));
    return discoveredMetadata;
  }

  private void enrichProviderOidcMetadata(
      WebClient webClient,
      Map<String, OidcDiscoveryResponse> discoveredMetadata,
      String providerId,
      OAuthProperties.OAuth2Provider provider
  ) {
    if (!StringUtils.hasText(provider.getIssuerUri())) {
      return;
    }

    final boolean discoveryRequired = !hasCompleteOidcEndpoints(provider);
    final String openIdConfigUri = provider.getIssuerUri().replaceAll("/$", "")
        + "/.well-known/openid-configuration";

    final OidcDiscoveryResponse metadata = resolveOidcMetadata(
        webClient,
        providerId,
        openIdConfigUri,
        discoveryRequired
    );
    if (metadata != null) {
      applyMissingEndpoints(provider, metadata);
      discoveredMetadata.put(providerId, metadata);
    }

    // Prevent OAuth2ClientPropertiesMapper from performing issuer discovery via RestTemplate,
    // which would ignore auth.oauth2.insecure-ssl and fail on self-signed certs.
    provider.setIssuerUri(null);
  }

  private OidcDiscoveryResponse resolveOidcMetadata(
      WebClient webClient,
      String providerId,
      String openIdConfigUri,
      boolean discoveryRequired
  ) {
    final OidcDiscoveryResponse metadata;
    try {
      metadata = webClient.get()
          .uri(openIdConfigUri)
          .retrieve()
          .bodyToMono(OidcDiscoveryResponse.class)
          .block(OIDC_DISCOVERY_TIMEOUT);
    } catch (Exception e) {
      if (!discoveryRequired) {
        log.warn(
            "Unable to resolve OIDC discovery metadata for provider [%s] from [%s]. "
                    .formatted(providerId, openIdConfigUri)
                + "Continuing because provider endpoints are fully specified; "
                + "OIDC logout endpoint may be unavailable.",
            e
        );
        return null;
      }
      throw new IllegalStateException(
          "Unable to resolve OIDC discovery metadata for provider [%s] from [%s]"
              .formatted(providerId, openIdConfigUri),
          e
      );
    }

    if (metadata == null) {
      if (!discoveryRequired) {
        log.warn(
            "Empty OIDC discovery metadata for provider [%s] from [%s]. "
                    .formatted(providerId, openIdConfigUri)
                + "Continuing because provider endpoints are fully specified; "
                + "OIDC logout endpoint may be unavailable."
        );
        return null;
      }
      throw new IllegalStateException(
          "Empty OIDC discovery metadata for provider [%s] from [%s]"
              .formatted(providerId, openIdConfigUri)
      );
    }
    return metadata;
  }

  private void applyMissingEndpoints(OAuthProperties.OAuth2Provider provider, OidcDiscoveryResponse metadata) {
    if (!StringUtils.hasText(provider.getAuthorizationUri())) {
      provider.setAuthorizationUri(metadata.authorizationEndpoint());
    }
    if (!StringUtils.hasText(provider.getTokenUri())) {
      provider.setTokenUri(metadata.tokenEndpoint());
    }
    if (!StringUtils.hasText(provider.getJwkSetUri())) {
      provider.setJwkSetUri(metadata.jwksUri());
    }
    if (!StringUtils.hasText(provider.getUserInfoUri())) {
      provider.setUserInfoUri(metadata.userInfoEndpoint());
    }
  }

  private boolean hasCompleteOidcEndpoints(OAuthProperties.OAuth2Provider provider) {
    return StringUtils.hasText(provider.getAuthorizationUri())
        && StringUtils.hasText(provider.getTokenUri())
        && StringUtils.hasText(provider.getJwkSetUri())
        && StringUtils.hasText(provider.getUserInfoUri());
  }

  private ClientRegistration enrichClientRegistrationWithOidcMetadata(
      ClientRegistration registration,
      OidcDiscoveryResponse metadata
  ) {
    if (metadata == null) {
      return registration;
    }

    Map<String, Object> providerMetadata = new HashMap<>(registration.getProviderDetails().getConfigurationMetadata());
    if (StringUtils.hasText(metadata.endSessionEndpoint())) {
      providerMetadata.put("end_session_endpoint", metadata.endSessionEndpoint());
    }
    if (StringUtils.hasText(metadata.issuer())) {
      providerMetadata.put("issuer", metadata.issuer());
    }

    if (providerMetadata.equals(registration.getProviderDetails().getConfigurationMetadata())
        && Objects.equals(registration.getProviderDetails().getIssuerUri(), metadata.issuer())) {
      return registration;
    }

    var builder = ClientRegistration.withClientRegistration(registration)
        .providerConfigurationMetadata(providerMetadata);
    if (StringUtils.hasText(metadata.issuer())) {
      builder.issuerUri(metadata.issuer());
    }
    return builder.build();
  }

  private record OidcDiscoveryResponse(
      @JsonProperty("issuer") String issuer,
      @JsonProperty("authorization_endpoint") String authorizationEndpoint,
      @JsonProperty("token_endpoint") String tokenEndpoint,
      @JsonProperty("jwks_uri") String jwksUri,
      @JsonProperty("userinfo_endpoint") String userInfoEndpoint,
      @JsonProperty("end_session_endpoint") String endSessionEndpoint
  ) {
  }

}
