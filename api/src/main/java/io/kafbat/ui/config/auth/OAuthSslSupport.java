package io.kafbat.ui.config.auth;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import reactor.netty.http.client.HttpClient;

/**
 * Applies a custom truststore (auth.oauth2.ssl) to the HTTP clients used to talk to OAuth2 providers.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OAuthSslSupport {

  @Nullable
  @SneakyThrows
  static TrustManagerFactory trustManagerFactory(@Nullable OAuthProperties.Ssl ssl) {
    if (ssl == null || ssl.getTruststoreLocation() == null) {
      return null;
    }
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (var in = new FileInputStream(ResourceUtils.getFile(ssl.getTruststoreLocation()))) {
      trustStore.load(in, ssl.getTruststorePassword() != null ? ssl.getTruststorePassword().toCharArray() : null);
    }
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    return trustManagerFactory;
  }

  @SneakyThrows
  static HttpClient configureSsl(HttpClient httpClient, @Nullable TrustManagerFactory trustManagerFactory) {
    if (trustManagerFactory == null) {
      return httpClient;
    }
    SslContext sslContext = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();
    return httpClient.secure(spec -> spec.sslContext(sslContext));
  }

  /**
   * Spring Security resolves "issuer-uri" metadata (.well-known/openid-configuration) through a static
   * RestTemplate inside {@link ClientRegistrations} that can't be customized via public API and always uses
   * the JVM default truststore. We swap its request factory so that discovery honours our truststore as well.
   */
  @SneakyThrows
  static void configureIssuerDiscoverySsl(@Nullable TrustManagerFactory trustManagerFactory) {
    if (trustManagerFactory == null) {
      return;
    }
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    issuerDiscoveryRestTemplate().setRequestFactory(new SslRequestFactory(sslContext.getSocketFactory()));
  }

  static RestTemplate issuerDiscoveryRestTemplate() {
    try {
      Field field = ClientRegistrations.class.getDeclaredField("rest");
      field.setAccessible(true);
      return (RestTemplate) field.get(null);
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new IllegalStateException(
          "Unable to apply auth.oauth2.ssl truststore to OAuth2 issuer discovery. "
              + "Consider specifying provider endpoints explicitly instead of issuer-uri.", e);
    }
  }

  private static class SslRequestFactory extends SimpleClientHttpRequestFactory {
    private final SSLSocketFactory socketFactory;

    SslRequestFactory(SSLSocketFactory socketFactory) {
      this.socketFactory = socketFactory;
    }

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
      if (connection instanceof HttpsURLConnection httpsConnection) {
        httpsConnection.setSSLSocketFactory(socketFactory);
      }
      super.prepareConnection(connection, httpMethod);
    }
  }
}
