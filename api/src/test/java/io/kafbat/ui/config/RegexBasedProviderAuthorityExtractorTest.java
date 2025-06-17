package io.kafbat.ui.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
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
