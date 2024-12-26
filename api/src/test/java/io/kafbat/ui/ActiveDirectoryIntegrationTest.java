package io.kafbat.ui;

import static io.kafbat.ui.AbstractIntegrationTest.LOCAL;
import static io.kafbat.ui.container.ActiveDirectoryContainer.DOMAIN;
import static io.kafbat.ui.container.ActiveDirectoryContainer.FIRST_USER_WITH_GROUP;
import static io.kafbat.ui.container.ActiveDirectoryContainer.PASSWORD;
import static io.kafbat.ui.container.ActiveDirectoryContainer.SECOND_USER_WITH_GROUP;
import static io.kafbat.ui.container.ActiveDirectoryContainer.USER_WITHOUT_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.container.ActiveDirectoryContainer;
import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.UserPermissionDTO;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest
@ActiveProfiles("rbac-ad")
@AutoConfigureWebTestClient(timeout = "60000")
@ContextConfiguration(initializers = {ActiveDirectoryIntegrationTest.Initializer.class})
public class ActiveDirectoryIntegrationTest {
  private static final String SESSION = "SESSION";

  private static final ActiveDirectoryContainer ACTIVE_DIRECTORY = new ActiveDirectoryContainer();

  @Autowired
  private WebTestClient webTestClient;

  @BeforeAll
  public static void setup() {
    ACTIVE_DIRECTORY.start();
  }

  @AfterAll
  public static void shutdown() {
    ACTIVE_DIRECTORY.stop();
  }

  @Test
  public void testUserPermissions() {
    AuthenticationInfoDTO info = authenticationInfo(FIRST_USER_WITH_GROUP);

    assertNotNull(info);
    assertTrue(info.getRbacEnabled());

    List<UserPermissionDTO> permissions = info.getUserInfo().getPermissions();

    assertFalse(permissions.isEmpty());
    assertTrue(permissions.stream().anyMatch(permission ->
        permission.getClusters().contains(LOCAL) && permission.getResource() == ResourceTypeDTO.TOPIC));
    assertEquals(permissions, authenticationInfo(SECOND_USER_WITH_GROUP).getUserInfo().getPermissions());
  }

  @Test
  public void testEmptyPermissions() {
    assertTrue(Objects.requireNonNull(authenticationInfo(USER_WITHOUT_GROUP))
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
                .body(BodyInserters.fromFormData("username", name).with("password", PASSWORD))
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
    public void initialize(@NotNull ConfigurableApplicationContext context) {
      System.setProperty("spring.ldap.urls", ACTIVE_DIRECTORY.getLdapUrl());
      System.setProperty("oauth2.ldap.activeDirectory", "true");
      System.setProperty("oauth2.ldap.activeDirectory.domain", DOMAIN);
    }
  }
}
