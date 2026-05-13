package io.kafbat.ui.config.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OAuthPropertiesConverterTest {

  @Test
  void shouldMapClientAuthenticationMethod() {
    var provider = new OAuthProperties.OAuth2Provider();
    provider.setProvider("dex");
    provider.setClientId("client-id");
    provider.setClientAuthenticationMethod("none");
    provider.setAuthorizationGrantType("authorization_code");
    provider.setScope(Set.of("openid"));
    provider.setCustomParams(Collections.emptyMap());

    var properties = new OAuthProperties();
    properties.setClient(Map.of("dex", provider));

    var result = OAuthPropertiesConverter.convertProperties(properties);

    assertThat(result.getRegistration())
        .containsKey("dex");
    assertThat(result.getRegistration().get("dex").getClientAuthenticationMethod())
        .isEqualTo("none");
  }
}
