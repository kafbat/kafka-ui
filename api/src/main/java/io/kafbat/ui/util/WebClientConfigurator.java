package io.kafbat.ui.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.ValidationException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.ResourceUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class WebClientConfigurator {

  private final WebClient.Builder builder = WebClient.builder();
  private HttpClient httpClient = HttpClient
      .create()
      .proxyWithSystemProperties();
  private ObjectMapper objectMapper = defaultOM();

  public WebClientConfigurator() {
    configureObjectMapper(objectMapper);
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

    return configureSsl(
        keystoreConfig != null ? keystoreConfig.getKeystoreLocation() : null,
        keystoreConfig != null ? keystoreConfig.getKeystorePassword() : null,
        truststoreConfig != null ? truststoreConfig.getTruststoreLocation() : null,
        truststoreConfig != null ? truststoreConfig.getTruststorePassword() : null
    );
  }

  @SneakyThrows
  private WebClientConfigurator configureSsl(
      @Nullable String keystoreLocation,
      @Nullable String keystorePassword,
      @Nullable String truststoreLocation,
      @Nullable String truststorePassword) {
    if (truststoreLocation == null && keystoreLocation == null) {
      return this;
    }

    SslContextBuilder contextBuilder = SslContextBuilder.forClient();
    if (truststoreLocation != null && truststorePassword != null) {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(
          new FileInputStream((ResourceUtils.getFile(truststoreLocation))),
          truststorePassword.toCharArray()
      );
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm()
      );
      trustManagerFactory.init(trustStore);
      contextBuilder.trustManager(trustManagerFactory);
    }

    // Prepare keystore only if we got a keystore
    if (keystoreLocation != null && keystorePassword != null) {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(
          new FileInputStream(ResourceUtils.getFile(keystoreLocation)),
          keystorePassword.toCharArray()
      );

      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
      contextBuilder.keyManager(keyManagerFactory);
    }

    // Create webclient
    SslContext context = contextBuilder.build();

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

  public WebClientConfigurator configureOAuth(@Nullable ClustersProperties.SchemaRegistryOauth oauth) {
    if (oauth != null && oauth.getTokenUrl() != null
        && oauth.getClientId() != null && oauth.getClientSecret() != null) {
      log.info("Configuring OAuth for Schema Registry with token URL: {}", oauth.getTokenUrl());
      builder.filter((request, next) -> {
        log.debug("OAuth filter: Fetching access token from {}", oauth.getTokenUrl());
        WebClient tokenClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        return tokenClient
            .post()
            .uri(oauth.getTokenUrl())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials&client_id=" + oauth.getClientId()
                + "&client_secret=" + oauth.getClientSecret())
            .retrieve()
            .bodyToMono(java.util.Map.class)
            .doOnNext(response -> log.debug("OAuth token response received: {}", response.keySet()))
            .flatMap(response -> {
              String accessToken = (String) response.get("access_token");
              if (accessToken == null) {
                log.error("OAuth token response does not contain 'access_token' field. Response keys: {}",
                    response.keySet());
                return reactor.core.publisher.Mono.error(
                    new RuntimeException("OAuth token response does not contain 'access_token' field"));
              }
              log.debug("OAuth access token obtained, adding Bearer token to request");
              var modifiedRequest = org.springframework.web.reactive.function.client.ClientRequest.from(request)
                  .headers(headers -> headers.setBearerAuth(accessToken))
                  .build();
              return next.exchange(modifiedRequest);
            })
            .onErrorResume(error -> {
              log.error("Failed to obtain OAuth access token from {}: {}", oauth.getTokenUrl(),
                  error.getMessage(), error);
              return reactor.core.publisher.Mono.error(
                  new RuntimeException("Failed to obtain OAuth access token from " + oauth.getTokenUrl()
                      + ": " + error.getMessage(), error));
            });
      });
    }
    return this;
  }

  public WebClientConfigurator configureBufferSize(DataSize maxBuffSize) {
    builder.codecs(c -> c.defaultCodecs().maxInMemorySize((int) maxBuffSize.toBytes()));
    return this;
  }

  public void configureObjectMapper(ObjectMapper mapper) {
    this.objectMapper = mapper;
    builder.codecs(codecs -> {
      codecs.defaultCodecs()
          .jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
      codecs.defaultCodecs()
          .jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
    });
  }

  public WebClientConfigurator configureAdditionalDecoderMediaTypes(MediaType... additionalMediaTypes) {
    builder.codecs(codecs -> {
      codecs.defaultCodecs()
          .jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
      MediaType[] allMediaTypes = Stream.concat(
          Stream.of(MediaType.APPLICATION_JSON),
          Stream.of(additionalMediaTypes)
      ).toArray(MediaType[]::new);
      codecs.defaultCodecs()
          .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, allMediaTypes));
    });
    return this;
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
