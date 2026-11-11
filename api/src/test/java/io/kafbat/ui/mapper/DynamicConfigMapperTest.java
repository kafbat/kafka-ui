package io.kafbat.ui.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.util.DynamicConfigOperations.PropertiesStructure;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.core.io.FileSystemResource;

class DynamicConfigMapperTest {

  @Test
  void resourceServerSurvivesRoundTrip() {
    var resourceServer = new OAuth2ResourceServerProperties();
    resourceServer.getJwt().setIssuerUri("https://issuer.example.com");
    resourceServer.getJwt().setJwkSetUri("https://issuer.example.com/jwks");
    resourceServer.getJwt().setAudiences(List.of("kafka-ui"));
    // toDto calls map(Resource) without a null check
    resourceServer.getJwt().setPublicKeyLocation(new FileSystemResource("public.pem"));
    resourceServer.getOpaquetoken().setIntrospectionUri("https://issuer.example.com/introspect");

    var oauth2 = new OAuthProperties();
    oauth2.setResourceServer(resourceServer);
    var properties = PropertiesStructure.builder()
        .auth(PropertiesStructure.Auth.builder().oauth2(oauth2).build())
        .build();

    var mapper = new DynamicConfigMapperImpl();
    var restored = mapper.fromDto(mapper.toDto(properties));

    assertThat(restored.getAuth().getOauth2().getResourceServer())
        .usingRecursiveComparison()
        .ignoringFields("jwt.publicKeyLocation")
        .isEqualTo(resourceServer);
  }
}
