package io.kafbat.ui.config.auth;

import io.kafbat.ui.model.ApplicationConfigDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "auth.type=OAUTH2",
    "dynamic.config.enabled=true",
    "rbac.roles[0].name=config-editor",
    "rbac.roles[0].clusters[0]=local",
    "rbac.roles[0].permissions[0].resource=applicationconfig",
    "rbac.roles[0].permissions[0].actions[0]=edit"
})
abstract class AbstractBearerTokenRbacTest {

  @Autowired
  private WebTestClient webTestClient;

  WebTestClient.ResponseSpec validateConfig(String token) {
    return webTestClient.put().uri("/api/config/validated")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .bodyValue(new ApplicationConfigDTO().properties(new ApplicationConfigPropertiesDTO()))
        .exchange();
  }
}
