package io.kafbat.ui.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.ValidationException;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

public class WebClientConfigurator {

  private final WebClient.Builder builder = WebClient.builder();
  private HttpClient httpClient = HttpClient
      .create()
      .proxyWithSystemProperties();

  public WebClientConfigurator() {
    configureObjectMapper(defaultOM());
  }

  private static ObjectMapper defaultOM() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new JsonNullableModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public WebClientConfigurator configureSsl(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                            @Nullable ClustersProperties.KeystoreConfig keystoreConfig) {
    if (truststoreConfig != null && !truststoreConfig.isVerify()) {
      return configureNoSsl();
    }

    SslBundle bundle = SslBundleUtil.loadBundle(truststoreConfig, keystoreConfig);
    if (bundle == null) {
      return configureNoSsl();
    }

    SSLContext sslContext = bundle.createSslContext();
    String[] ciphers = sslContext.createSSLEngine().getSupportedCipherSuites();
    JdkSslContext context = new JdkSslContext(
        sslContext,
        true,
        Arrays.asList(ciphers),
        IdentityCipherSuiteFilter.INSTANCE,
        null,
        ClientAuth.NONE,
        null,
        false
    );

    httpClient = httpClient.secure(t -> t.sslContext(context));
    return this;
  }

  @SneakyThrows
  public WebClientConfigurator configureNoSsl() {
    var contextBuilder = SslContextBuilder.forClient();
    contextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);

    SslContext context = contextBuilder.build();

    httpClient = httpClient.secure(t -> t.sslContext(context));
    return this;
  }

  public WebClientConfigurator configureBasicAuth(@Nullable String username, @Nullable String password) {
    if (username != null && password != null) {
      builder.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password));
    } else if (username != null) {
      throw new ValidationException("You specified username but did not specify password");
    } else if (password != null) {
      throw new ValidationException("You specified password but did not specify username");
    }
    return this;
  }

  public WebClientConfigurator configureBufferSize(DataSize maxBuffSize) {
    builder.codecs(c -> c.defaultCodecs().maxInMemorySize((int) maxBuffSize.toBytes()));
    return this;
  }

  public void configureObjectMapper(ObjectMapper mapper) {
    builder.codecs(codecs -> {
      codecs.defaultCodecs()
          .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
      codecs.defaultCodecs()
          .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
    });
  }

  public WebClientConfigurator configureCodecs(Consumer<ClientCodecConfigurer> configurer) {
    builder.codecs(configurer);
    return this;
  }

  public WebClientConfigurator configureResponseTimeout(Duration responseTimeout) {
    httpClient = httpClient.responseTimeout(responseTimeout);
    return this;
  }

  public WebClient build() {
    return builder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }
}
