package io.kafbat.ui.config.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "auth.oauth2.resource-server.opaquetoken.client-id=kafbat-ui",
    "auth.oauth2.resource-server.opaquetoken.client-secret=secret",
    "rbac.roles[0].subjects[0].provider=oauth",
    "rbac.roles[0].subjects[0].type=role",
    "rbac.roles[0].subjects[0].value=SCOPE_kafka-ui-config",
    "rbac.roles[0].subjects[1].provider=oauth",
    "rbac.roles[0].subjects[1].type=user",
    "rbac.roles[0].subjects[1].value=ci-bot"
})
class OAuthOpaqueTokenRbacTest extends AbstractBearerTokenRbacTest {

  private static final WireMockServer INTROSPECTION = startIntrospection();

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
  void tokenWithMatchingSubIsAllowed() {
    validateConfig("matching-sub").expectStatus().isOk();
  }

  @Test
  void tokenWithoutMatchingRoleIsForbidden() {
    validateConfig("without-role").expectStatus().isForbidden();
  }

  private static WireMockServer startIntrospection() {
    var server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    server.start();
    stubIntrospection(server, "with-role", "service-account", "kafka-ui-config");
    stubIntrospection(server, "without-role", "service-account", "other");
    stubIntrospection(server, "matching-sub", "ci-bot", "other");
    return server;
  }

  private static void stubIntrospection(WireMockServer server, String token, String sub, String scope) {
    server.stubFor(post("/introspect")
        .withRequestBody(containing("token=" + token))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"active\": true, \"sub\": \"" + sub + "\", \"scope\": \"" + scope + "\"}")));
  }
}
