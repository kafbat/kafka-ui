package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kafbat.ui.model.ApplicationConfigDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "auth.type=OAUTH2",
    "auth.oauth2.resource-server.opaquetoken.client-id=kafbat-ui",
    "auth.oauth2.resource-server.opaquetoken.client-secret=secret",
    "auth.oauth2.resource-server-rbac.roles-claim=roles",
    "rbac.roles[0].name=config-editor",
    "rbac.roles[0].clusters[0]=local",
    "rbac.roles[0].subjects[0].provider=oauth",
    "rbac.roles[0].subjects[0].type=role",
    "rbac.roles[0].subjects[0].value=kafka-ui-config",
    "rbac.roles[0].permissions[0].resource=applicationconfig",
    "rbac.roles[0].permissions[0].actions[0]=edit"
})
class OAuthOpaqueTokenRbacTest {

  private static final WireMockServer INTROSPECTION = startIntrospection();

  @Autowired
  private WebTestClient webTestClient;

  @DynamicPropertySource
  static void introspectionUri(DynamicPropertyRegistry registry) {
    registry.add("auth.oauth2.resource-server.opaquetoken.introspection-uri",
        () -> INTROSPECTION.baseUrl() + "/introspect");
  }

  @AfterAll
  static void stopIntrospection() {
    INTROSPECTION.stop();
  }

  @Test
  void tokenWithMatchingRoleIsAllowed() {
    validateConfig("with-role").expectStatus().isOk();
  }

  @Test
  void tokenWithoutMatchingRoleIsForbidden() {
    validateConfig("without-role").expectStatus().isForbidden();
  }

  private WebTestClient.ResponseSpec validateConfig(String token) {
    return webTestClient.put().uri("/api/config/validated")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .bodyValue(new ApplicationConfigDTO().properties(new ApplicationConfigPropertiesDTO()))
        .exchange();
  }

  private static WireMockServer startIntrospection() {
    var server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    stubIntrospection(server, "with-role", "[\"kafka-ui-config\"]");
    stubIntrospection(server, "without-role", "[\"other\"]");
    return server;
  }

  private static void stubIntrospection(WireMockServer server, String token, String roles) {
    server.stubFor(post("/introspect")
        .withRequestBody(containing("token=" + token))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"active\": true, \"sub\": \"service-account\", \"roles\": " + roles + "}")));
  }
}
