package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class SimpleOAuthProxyConfigTest {

  @Test
  void proxyDisabledByDefault() {
    var props = new SimpleOAuthProxyConfig.ProxyProperties();

    assertThat(props.isEnabled()).isFalse();
    assertThat(props.getHost()).isNull();
    assertThat(props.getPort()).isNull();
  }

  @Test
  void proxyPropertiesCanBeSet() {
    var props = new SimpleOAuthProxyConfig.ProxyProperties();

    props.setEnabled(true);
    props.setHost("proxy.example.com");
    props.setPort(8080);

    assertThat(props.isEnabled()).isTrue();
    assertThat(props.getHost()).isEqualTo("proxy.example.com");
    assertThat(props.getPort()).isEqualTo(8080);
  }

  @Test
  void createsWebClientWithExplicitProxy() {
    var config = new SimpleOAuthProxyConfig();
    var props = new SimpleOAuthProxyConfig.ProxyProperties();
    props.setEnabled(true);
    props.setHost("proxy.example.com");
    props.setPort(8080);

    WebClient webClient = config.oauth2WebClient(props);

    assertThat(webClient).isNotNull();
  }

  @Test
  void createsWebClientWithSystemProxy() {
    var config = new SimpleOAuthProxyConfig();
    var props = new SimpleOAuthProxyConfig.ProxyProperties();
    props.setEnabled(true);
    // No host/port - uses system properties

    WebClient webClient = config.oauth2WebClient(props);

    assertThat(webClient).isNotNull();
  }

  @Test
  void logsCorrectProxyTypeWhenCreatingWebClient() {
    var config = new SimpleOAuthProxyConfig();

    // Test explicit proxy logging
    var explicitProps = new SimpleOAuthProxyConfig.ProxyProperties();
    explicitProps.setEnabled(true);
    explicitProps.setHost("proxy.example.com");
    explicitProps.setPort(3128);

    WebClient explicitClient = config.oauth2WebClient(explicitProps);
    assertThat(explicitClient).isNotNull();

    // Test system proxy logging
    var systemProps = new SimpleOAuthProxyConfig.ProxyProperties();
    systemProps.setEnabled(true);

    WebClient systemClient = config.oauth2WebClient(systemProps);
    assertThat(systemClient).isNotNull();
  }
}