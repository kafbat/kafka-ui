package io.kafbat.ui;

import static io.kafbat.ui.AbstractIntegrationTest.LOCAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.api.model.Action;
import io.kafbat.ui.container.OpenLdapContainer;
import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.UserPermissionDTO;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@Slf4j
@SpringBootTest
@ActiveProfiles("rbac-ldap")
@AutoConfigureWebTestClient(timeout = "60000")
@ContextConfiguration(initializers = {OpenLdapTest.Initializer.class})
@DirtiesContext
class OpenLdapTest {
  private static final String SESSION = "SESSION";
  private static final OpenLdapContainer LDAP_CONTAINER = new OpenLdapContainer();

  @Autowired
  private WebTestClient webTestClient;

  @BeforeAll
  static void setup() {
    LDAP_CONTAINER.start();
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    LDAP_CONTAINER.followOutput(logConsumer);
  }

  @AfterAll
  static void shutdown() {
    LDAP_CONTAINER.stop();
  }

  @Test
  public void testUserPermissions() {
    AuthenticationInfoDTO info = authenticationInfo("johndoe");

    assertNotNull(info);
    assertTrue(info.getRbacEnabled());
    List<UserPermissionDTO> permissions = info.getUserInfo().getPermissions();
    assertFalse(permissions.isEmpty());
    assertTrue(permissions.stream().anyMatch(permission ->
        permission.getClusters().contains(LOCAL)
            && permission.getResource() == ResourceTypeDTO.TOPIC
            && permission.getActions().stream()
                .allMatch(action -> Action.fromValue(action.getValue()) != Action.ALL)
        )
    );
    assertEquals(permissions, authenticationInfo("johnwick").getUserInfo().getPermissions());
    assertEquals(permissions, authenticationInfo("jacksmith").getUserInfo().getPermissions());
  }

  @Test
  public void testEmptyPermissions() {
    assertTrue(Objects.requireNonNull(authenticationInfo("johnjames"))
        .getUserInfo()
        .getPermissions()
        .isEmpty()
    );
  }

  private String session(String name) {
    return Objects.requireNonNull(
            webTestClient
                .post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", name).with("password", name + "@kafbat.io"))
                .exchange()
                .expectStatus()
                .isFound()
                .returnResult(String.class)
                .getResponseCookies()
                .getFirst(SESSION))
        .getValue();
  }

  private AuthenticationInfoDTO authenticationInfo(String name) {
    return webTestClient
        .get()
        .uri("/api/authorization")
        .cookie(SESSION, session(name))
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(AuthenticationInfoDTO.class)
        .getResponseBody()
        .blockFirst();
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
      System.setProperty("spring.ldap.urls", LDAP_CONTAINER.getLdapUrl());
      System.setProperty("oauth2.ldap.activeDirectory", "false");
    }
  }
}
