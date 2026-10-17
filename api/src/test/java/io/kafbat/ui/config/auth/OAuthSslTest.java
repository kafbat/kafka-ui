package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.File;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.test.TestSslUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Verifies that auth.oauth2.ssl truststore is used when talking to an OAuth2 provider
 * secured with a self-signed certificate.
 */
class OAuthSslTest {

  private static final String PASSWORD = "changeit";
  private static final String REGISTRATION_ID = "keycloak";

  private static WireMockServer idpServer;
  private static File truststore;

  @BeforeAll
  static void startIdp() throws Exception {
    KeyPair keyPair = TestSslUtils.generateKeyPair("RSA");
    X509Certificate cert = new TestSslUtils.CertificateBuilder(365, "SHA256withRSA")
        .sanDnsNames("localhost")
        .sanIpAddress(InetAddress.getByName("localhost"))
        .generate("CN=localhost", keyPair);

    File keystore = File.createTempFile("idp-keystore", ".jks");
    keystore.deleteOnExit();
    TestSslUtils.createKeyStore(keystore.getPath(), new Password(PASSWORD), new Password(PASSWORD),
        "idp", keyPair.getPrivate(), cert);

    truststore = File.createTempFile("idp-truststore", ".jks");
    truststore.deleteOnExit();
    TestSslUtils.createTrustStore(truststore.getPath(), new Password(PASSWORD), Map.of("idp", cert));

    idpServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
        .httpDisabled(true)
        .dynamicHttpsPort()
        .keystorePath(keystore.getPath())
        .keystorePassword(PASSWORD)
        .keyManagerPassword(PASSWORD)
        .keystoreType("JKS"));
    idpServer.start();

    String issuer = issuer();
    idpServer.stubFor(get(urlPathEqualTo("/.well-known/openid-configuration"))
        .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody("""
                {
                  "issuer": "%1$s",
                  "authorization_endpoint": "%1$s/auth",
                  "token_endpoint": "%1$s/token",
                  "userinfo_endpoint": "%1$s/userinfo",
                  "jwks_uri": "%1$s/certs",
                  "response_types_supported": ["code"],
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"]
                }""".formatted(issuer))));
    idpServer.stubFor(get(urlPathEqualTo("/certs"))
        .willReturn(aResponse()
            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"keys\":[]}")));
  }

  @AfterAll
  static void stopIdp() {
    if (idpServer != null) {
      idpServer.stop();
    }
  }

  @AfterEach
  void resetIssuerDiscoveryClient() {
    // issuer discovery client is a JVM-wide static, restoring default to not affect other tests
    OAuthSslSupport.issuerDiscoveryRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory());
  }

  @Test
  void issuerDiscoveryFailsWithoutTruststore() {
    var config = new OAuthSecurityConfig(oauthProperties(null));

    assertThatThrownBy(config::clientRegistrationRepository)
        .hasStackTraceContaining("PKIX path building failed");
  }

  @Test
  void issuerDiscoveryUsesConfiguredTruststore() {
    var config = new OAuthSecurityConfig(oauthProperties(sslProperties()));

    ClientRegistration registration = config.clientRegistrationRepository()
        .findByRegistrationId(REGISTRATION_ID)
        .block();

    assertThat(registration).isNotNull();
    assertThat(registration.getProviderDetails().getIssuerUri()).isEqualTo(issuer());
    assertThat(registration.getProviderDetails().getTokenUri()).isEqualTo(issuer() + "/token");
    assertThat(registration.getProviderDetails().getJwkSetUri()).isEqualTo(issuer() + "/certs");
  }

  @Test
  void webClientFailsWithoutTruststore() {
    WebClient webClient = new OAuthSecurityConfig(oauthProperties(null)).oauthWebClient();

    assertThatThrownBy(() -> fetchJwks(webClient))
        .hasStackTraceContaining("PKIX path building failed");
  }

  @Test
  void webClientUsesConfiguredTruststore() {
    WebClient webClient = new OAuthSecurityConfig(oauthProperties(sslProperties())).oauthWebClient();

    assertThat(fetchJwks(webClient)).isEqualTo("{\"keys\":[]}");
  }

  private static String fetchJwks(WebClient webClient) {
    return webClient.get().uri(issuer() + "/certs").retrieve().bodyToMono(String.class).block();
  }

  private static String issuer() {
    return "https://localhost:" + idpServer.httpsPort();
  }

  private static OAuthProperties.Ssl sslProperties() {
    var ssl = new OAuthProperties.Ssl();
    ssl.setTruststoreLocation(truststore.getPath());
    ssl.setTruststorePassword(PASSWORD);
    return ssl;
  }

  private static OAuthProperties oauthProperties(OAuthProperties.Ssl ssl) {
    var provider = new OAuthProperties.OAuth2Provider();
    provider.setProvider(REGISTRATION_ID);
    provider.setClientId("kafka-ui");
    provider.setClientSecret("secret");
    provider.setIssuerUri(issuer());

    var properties = new OAuthProperties();
    properties.getClient().put(REGISTRATION_ID, provider);
    properties.setSsl(ssl);
    properties.init();
    return properties;
  }
}
