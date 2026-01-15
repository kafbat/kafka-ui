package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.kafbat.ui.config.auth.logout.OAuthLogoutSuccessHandler;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

/**
 * Shared test support for OAuth2 proxy integration tests.
 *
 * <p>Uses TWO WireMock instances:
 * <ul>
 *   <li>proxyServer - Acts as HTTP proxy, forwards requests to oauthServer</li>
 *   <li>oauthServer - Acts as the OAuth provider (token endpoint, userinfo, etc.)</li>
 * </ul>
 *
 * <p>The proxy uses WireMock's proxiedFrom() to forward requests.
 * Tests verify requests arrive at BOTH servers (with proxy) or ONLY OAuth server (without proxy).
 */
public final class OAuthTestSupport {

  // OAuth endpoint paths
  public static final String TOKEN_PATH = "/oauth/token";
  public static final String USERINFO_PATH = "/oauth/userinfo";
  public static final String INTROSPECT_PATH = "/oauth/introspect";
  public static final String JWKS_PATH = "/.well-known/jwks.json";
  public static final String AUTHORIZE_PATH = "/oauth/authorize";

  // Test client credentials
  public static final String CLIENT_ID = "test-client";
  public static final String CLIENT_SECRET = "test-secret";
  public static final String REGISTRATION_ID = "test";

  @Getter
  private static WireMockServer proxyServer;
  private static WireMockServer oauthServer;
  @Getter
  private static RSAKey rsaKey;
  private static boolean initialized = false;
  @Getter
  private static boolean proxyEnabled = false;

  private OAuthTestSupport() {
  }

  /**
   * Initialize both WireMock servers.
   *
   * @param enableProxy if true, sets system properties to route traffic through proxy
   */
  public static synchronized void ensureStarted(boolean enableProxy) {
    if (!initialized) {
      try {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
      } catch (Exception e) {
        throw new RuntimeException("Failed to generate RSA key", e);
      }

      // OAuth server - the actual destination
      oauthServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
      oauthServer.start();

      // Proxy server - forwards to OAuth server
      proxyServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
      proxyServer.start();

      // Configure proxy to forward all requests to OAuth server
      proxyServer.stubFor(WireMock.any(urlMatching(".*"))
          .willReturn(aResponse().proxiedFrom("http://localhost:" + oauthServer.port())));

      initialized = true;
    }

    // Intentionally outside the initialized block - different tests may toggle proxy on/off
    // Set or clear proxy system properties based on enableProxy
    if (enableProxy) {
      System.setProperty("http.proxyHost", "localhost");
      System.setProperty("http.proxyPort", String.valueOf(proxyServer.port()));
      // Override default nonProxyHosts ("localhost|127.*|[::1]") to allow proxying localhost for tests
      System.setProperty("http.nonProxyHosts", "none");
    } else {
      System.clearProperty("http.proxyHost");
      System.clearProperty("http.proxyPort");
      System.clearProperty("http.nonProxyHosts");
    }
    proxyEnabled = enableProxy;
  }

  public static void stopServers() {
    System.clearProperty("http.proxyHost");
    System.clearProperty("http.proxyPort");
    System.clearProperty("http.nonProxyHosts");
    if (proxyServer != null) {
      proxyServer.stop();
      proxyServer = null;
    }
    if (oauthServer != null) {
      oauthServer.stop();
      oauthServer = null;
    }
    initialized = false;
    proxyEnabled = false;
  }

  public static void resetServers() {
    if (oauthServer != null) {
      oauthServer.resetAll();
    }
    if (proxyServer != null) {
      proxyServer.resetAll();
    }
    // Re-stub proxy forwarding after reset
    if (proxyServer != null && oauthServer != null) {
      proxyServer.stubFor(WireMock.any(urlMatching(".*"))
          .willReturn(aResponse().proxiedFrom("http://localhost:" + oauthServer.port())));
    }
  }

  public static WireMockServer getOAuthServer() {
    return oauthServer;
  }

  public static String oauthBaseUrl() {
    return "http://localhost:" + oauthServer.port();
  }

  public static ClientRegistration testClientRegistration() {
    return ClientRegistration.withRegistrationId(REGISTRATION_ID)
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "profile", "email")
        .authorizationUri(oauthBaseUrl() + AUTHORIZE_PATH)
        .tokenUri(oauthBaseUrl() + TOKEN_PATH)
        .userInfoUri(oauthBaseUrl() + USERINFO_PATH)
        .jwkSetUri(oauthBaseUrl() + JWKS_PATH)
        .userNameAttributeName("sub")
        .build();
  }

  /**
   * Creates a GitHub-style client registration WITHOUT "openid" scope.
   * This tests the non-OIDC OAuth2 flow where no ID token is returned.
   */
  public static ClientRegistration githubStyleClientRegistration() {
    return ClientRegistration.withRegistrationId("github")
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("read:user")  // No "openid" scope - this is key for non-OIDC
        .authorizationUri(oauthBaseUrl() + AUTHORIZE_PATH)
        .tokenUri(oauthBaseUrl() + TOKEN_PATH)
        .userInfoUri(oauthBaseUrl() + USERINFO_PATH)
        .userNameAttributeName("login")
        .build();
  }

  public static OAuthProperties createOAuthProperties() {
    OAuthProperties props = mock(OAuthProperties.class);
    OAuthProperties.OAuth2Provider provider = mock(OAuthProperties.OAuth2Provider.class);
    when(provider.getProvider()).thenReturn(REGISTRATION_ID);
    Map<String, OAuthProperties.OAuth2Provider> clients = new HashMap<>();
    clients.put(REGISTRATION_ID, provider);
    when(props.getClient()).thenReturn(clients);
    when(props.getResourceServer()).thenReturn(null);
    return props;
  }

  /**
   * Initializer that starts WireMock servers WITH proxy enabled.
   */
  public static class WithProxyInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      ensureStarted(true);
    }
  }

  /**
   * Initializer that starts WireMock servers WITHOUT proxy enabled.
   */
  public static class WithoutProxyInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      ensureStarted(false);
    }
  }

  /**
   * Abstract base configuration with common test beans.
   */
  @Import(OAuthSecurityConfig.class)
  public abstract static class AbstractTestConfig {
    @Bean(name = "testOAuthProperties")
    @Primary
    public abstract OAuthProperties authProperties();

    @Bean
    @Primary
    public AccessControlService accessControlService() {
      AccessControlService acs = mock(AccessControlService.class);
      when(acs.getOauthExtractors()).thenReturn(Collections.emptySet());
      return acs;
    }

    @Bean
    @Primary
    public ReactiveClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryReactiveClientRegistrationRepository(testClientRegistration());
    }

    @Bean(name = "testLogoutHandler")
    @Primary
    public ServerLogoutSuccessHandler defaultOidcLogoutHandler() {
      return mock(ServerLogoutSuccessHandler.class);
    }

    @Bean
    @Primary
    public OAuthLogoutSuccessHandler logoutSuccessHandler(
        @Qualifier("testOAuthProperties") OAuthProperties props,
        @Qualifier("testLogoutHandler") ServerLogoutSuccessHandler handler) {
      return new OAuthLogoutSuccessHandler(props, Collections.emptyList(), handler);
    }
  }

  @TestConfiguration
  public static class BaseTestConfig extends AbstractTestConfig {
    @Override
    public OAuthProperties authProperties() {
      return createOAuthProperties();
    }
  }
}
