package io.kafbat.ui.config.auth;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "auth.type", havingValue = "OAUTH2")
@EnableConfigurationProperties(SimpleOAuthProxyConfig.ProxyProperties.class)
public class SimpleOAuthProxyConfig {

  @Data
  @ConfigurationProperties("auth.oauth2.proxy")
  public static class ProxyProperties {
    private boolean enabled = false;
    private String host;
    private Integer port;
  }

  @Bean(name = "oauth2WebClient")
  @ConditionalOnProperty(value = "auth.oauth2.proxy.enabled", havingValue = "true")
  public WebClient oauth2WebClient(ProxyProperties proxyProperties) {
    HttpClient httpClient;

    if (proxyProperties.getHost() != null && proxyProperties.getPort() != null) {
      // Use explicit proxy configuration
      log.info("OAuth2 configured with explicit proxy: {}:{}",
          proxyProperties.getHost(), proxyProperties.getPort());

      httpClient = HttpClient.create()
          .proxy(proxy -> proxy
              .type(reactor.netty.transport.ProxyProvider.Proxy.HTTP)
              .host(proxyProperties.getHost())
              .port(proxyProperties.getPort()));
    } else {
      // Use system proxy properties
      log.info("OAuth2 configured to use system proxy properties");
      httpClient = HttpClient.create().proxyWithSystemProperties();
    }

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}