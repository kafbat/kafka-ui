package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Tests OAuth/OIDC configuration behavior for insecure SSL mode.
 *
 * <p>Focuses on:
 * <ul>
 *   <li>Defaulting OAuth2 registration values that were previously resolved via issuer discovery flow</li>
 *   <li>Building client registration from OIDC discovery metadata when insecure SSL is enabled</li>
 * </ul>
 */
class OAuthSecurityConfigInsecureSslTest {

  private WireMockServer oidcServer;

  @AfterEach
  void tearDown() {
    if (oidcServer != null) {
      oidcServer.stop();
    }
  }

  /**
   * Verifies compatibility defaults for OAuth2 registration fields.
   * <p>If not provided in config, authorization grant type and redirect URI
   * should fall back to the same values expected by authorization_code flow.</p>
   */
  @Test
  void oauthPropertiesApplyDefaultAuthorizationCodeAndRedirectUri() {
    OAuthProperties properties = new OAuthProperties();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setProvider("keycloak");
    provider.setClientId("client-id");
    properties.setClient(Map.of("keycloak", provider));

    properties.init();

    OAuthProperties.OAuth2Provider configured = properties.getClient().get("keycloak");
    assertThat(configured.getAuthorizationGrantType())
        .isEqualTo(OAuthProperties.DEFAULT_AUTHORIZATION_GRANT_TYPE);
    assertThat(configured.getRedirectUri())
        .isEqualTo(OAuthProperties.DEFAULT_REDIRECT_URI);
  }

  /**
   * Verifies that when insecure SSL is enabled and issuer-uri is configured,
   * OIDC metadata is resolved and used to build client registration endpoints.
   */
  @Test
  void insecureSslWithIssuerUriBuildsRegistrationFromOidcDiscovery() {
    oidcServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    oidcServer.start();

    String issuer = "http://localhost:" + oidcServer.port() + "/realms/test";
    String metadata = """
        {
          "issuer": "%s",
          "authorization_endpoint": "%s/oauth2/authorize",
          "token_endpoint": "%s/oauth2/token",
          "jwks_uri": "%s/.well-known/jwks.json",
          "userinfo_endpoint": "%s/oauth2/userinfo"
        }
        """.formatted(issuer, issuer, issuer, issuer, issuer);

    oidcServer.stubFor(get(urlEqualTo("/realms/test/.well-known/openid-configuration"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(metadata)));

    OAuthProperties properties = new OAuthProperties();
    properties.setInsecureSsl(true);

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setProvider("keycloak");
    provider.setClientId("client-id");
    provider.setClientSecret("client-secret");
    provider.setIssuerUri(issuer);
    properties.setClient(Map.of("keycloak", provider));
    properties.init();

    OAuthSecurityConfig config = new OAuthSecurityConfig(properties);
    var repository = config.clientRegistrationRepository(config.oauthWebClient());
    var registration = repository.findByRegistrationId("keycloak").block();

    assertThat(registration).isNotNull();
    assertThat(registration.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
    assertThat(registration.getRedirectUri()).isEqualTo(OAuthProperties.DEFAULT_REDIRECT_URI);
    assertThat(registration.getProviderDetails().getAuthorizationUri()).isEqualTo(issuer + "/oauth2/authorize");
    assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo(issuer + "/oauth2/token");
    assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo(issuer + "/.well-known/jwks.json");
    assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUri()).isEqualTo(issuer + "/oauth2/userinfo");
  }
}
