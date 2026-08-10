package io.kafbat.ui.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.CognitoAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GithubAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GoogleAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(RoleBasedAccessControlProperties.class)
@TestPropertySource(
    locations = "classpath:application-roles-definition.yml",
    factory = YamlPropertySourceFactory.class
)
public class RegexBasedProviderAuthorityExtractorTest {

  @Autowired
  private RoleBasedAccessControlProperties properties;
  private AccessControlService accessControlService;

  @BeforeEach
  public void configure() {
    this.accessControlService = new AccessControlServiceMock(properties.getRoles()).getMock();
  }

  @SneakyThrows
  @Test
  void extractOauth2Authorities() {

    ProviderAuthorityExtractor extractor = new OauthAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("role_definition", Set.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", "john@kafka.com"),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertNotNull(roles);
    assertEquals(Set.of("viewer", "admin"), roles);
    assertFalse(roles.contains("no one's role"));

  }

  @SneakyThrows
  @Test()
  void extractOauth2Authorities_blankEmail() {

    ProviderAuthorityExtractor extractor = new OauthAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("role_definition", Set.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", ""),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertNotNull(roles);
    assertFalse(roles.contains("viewer"));
    assertTrue(roles.contains("admin"));

  }

  @SneakyThrows
  @Test
  void extractCognitoAuthorities() {

    ProviderAuthorityExtractor extractor = new CognitoAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("cognito:groups", List.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", "john@kafka.com"),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of());
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertNotNull(roles);
    assertEquals(Set.of("viewer", "admin"), roles);
    assertFalse(roles.contains("no one's role"));

  }

  @SneakyThrows
  @Test
  void extractGithubAuthorities() {

    ProviderAuthorityExtractor extractor = new GithubAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("login", "john@kafka.com"),
        "login");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    additionalParams.put("provider", provider);

    additionalParams.put("request", new OAuth2UserRequest(
        withRegistrationId("registration-1")
            .clientId("client-1")
            .clientSecret("secret")
            .redirectUri("https://client.com")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri("https://provider.com/oauth2/authorization")
            .tokenUri("https://provider.com/oauth2/token")
            .clientName("Client 1")
            .build(),
        new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "XXXX", Instant.now(),
            Instant.now().plus(10, ChronoUnit.HOURS))));

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertNotNull(roles);
    assertEquals(Set.of("viewer"), roles);
    assertFalse(roles.contains("no one's role"));

  }

  @SneakyThrows
  @Test
  void extractGithubOrganizationAuthoritiesUsesProvidedProxyWebClient() {
    WireMockServer githubServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    WireMockServer proxyServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    githubServer.start();
    proxyServer.start();

    try {
      githubServer.stubFor(get(urlPathEqualTo("/user/orgs"))
          .willReturn(aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody("[{\"login\":\"open-metadata\"}]")));
      proxyServer.stubFor(WireMock.any(urlMatching(".*"))
          .willReturn(aResponse().proxiedFrom(githubServer.baseUrl())));

      ProviderAuthorityExtractor extractor = new GithubAuthorityExtractor();
      AccessControlService acs = new AccessControlServiceMock(List.of(githubOrganizationRole())).getMock();

      OAuth2User oauth2User = new DefaultOAuth2User(
          AuthorityUtils.createAuthorityList("SCOPE_read:org"),
          Map.of("login", "john@kafka.com", "organizations_url", githubServer.baseUrl() + "/orgs"),
          "login");

      WebClient proxyWebClient = WebClient.builder()
          .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
              .proxy(proxy -> proxy
                  .type(ProxyProvider.Proxy.HTTP)
                  .host("localhost")
                  .port(proxyServer.port()))))
          .build();

      HashMap<String, Object> additionalParams = new HashMap<>();
      additionalParams.put("provider", new OAuthProperties.OAuth2Provider());
      additionalParams.put(ProviderAuthorityExtractor.OAUTH_WEB_CLIENT, proxyWebClient);
      additionalParams.put("request", new OAuth2UserRequest(
          withRegistrationId("github")
              .clientId("client-1")
              .clientSecret("secret")
              .redirectUri("https://client.com")
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .authorizationUri(githubServer.baseUrl() + "/oauth2/authorization")
              .tokenUri(githubServer.baseUrl() + "/oauth2/token")
              .userInfoUri(githubServer.baseUrl() + "/user")
              .userNameAttributeName("login")
              .clientName("GitHub")
              .build(),
          new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "XXXX", Instant.now(),
              Instant.now().plus(10, ChronoUnit.HOURS))));

      Set<String> roles = extractor.extract(acs, oauth2User, additionalParams).block();

      assertNotNull(roles);
      assertEquals(Set.of("github-org-viewer"), roles);
      githubServer.verify(getRequestedFor(urlPathEqualTo("/user/orgs")));
      proxyServer.verify(getRequestedFor(urlPathEqualTo("/user/orgs")));
    } finally {
      proxyServer.stop();
      githubServer.stop();
    }
  }

  private Role githubOrganizationRole() {
    Subject subject = new Subject();
    subject.setProvider(Provider.OAUTH_GITHUB);
    subject.setType("organization");
    subject.setValue("open-metadata");

    Role role = new Role();
    role.setName("github-org-viewer");
    role.setClusters(List.of("local"));
    role.setSubjects(List.of(subject));
    return role;
  }

  @SneakyThrows
  @Test
  void extractGoogleAuthorities() {

    ProviderAuthorityExtractor extractor = new GoogleAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("hd", "memelord.lol", "email", "john@kafka.com"),
        "email");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertNotNull(roles);
    assertEquals(Set.of("viewer", "admin"), roles);
    assertFalse(roles.contains("no one's role"));

  }

}
