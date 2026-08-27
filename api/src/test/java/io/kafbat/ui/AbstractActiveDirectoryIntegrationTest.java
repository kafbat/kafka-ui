package io.kafbat.ui;

import static io.kafbat.ui.AbstractIntegrationTest.LOCAL;
import static io.kafbat.ui.container.ActiveDirectoryContainer.EMPTY_PERMISSIONS_USER;
import static io.kafbat.ui.container.ActiveDirectoryContainer.FIRST_USER_WITH_GROUP;
import static io.kafbat.ui.container.ActiveDirectoryContainer.PASSWORD;
import static io.kafbat.ui.container.ActiveDirectoryContainer.SECOND_USER_WITH_GROUP;
import static io.kafbat.ui.container.ActiveDirectoryContainer.USER_WITHOUT_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.UserPermissionDTO;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest
@ActiveProfiles("rbac-ad")
@AutoConfigureWebTestClient(timeout = "60000")
public abstract class AbstractActiveDirectoryIntegrationTest {
  public static final String SESSION = "SESSION";

  protected static void checkUserPermissions(WebTestClient client) {
    AuthenticationInfoDTO info = authenticationInfo(client, FIRST_USER_WITH_GROUP);

    assertNotNull(info);
    assertTrue(info.getRbacEnabled());

    List<UserPermissionDTO> permissions = info.getUserInfo().getPermissions();

    assertFalse(permissions.isEmpty());
    assertTrue(permissions.stream().anyMatch(permission ->
        permission.getClusters().contains(LOCAL) && permission.getResource() == ResourceTypeDTO.TOPIC));
    assertEquals(permissions, authenticationInfo(client, SECOND_USER_WITH_GROUP).getUserInfo().getPermissions());
    assertEquals(permissions, authenticationInfo(client, USER_WITHOUT_GROUP).getUserInfo().getPermissions());
  }

  protected static void checkEmptyPermissions(WebTestClient client) {
    assertTrue(Objects.requireNonNull(authenticationInfo(client, EMPTY_PERMISSIONS_USER))
        .getUserInfo()
        .getPermissions()
        .isEmpty()
    );
  }

  public static String session(WebTestClient client, String name) {
    return Objects.requireNonNull(
            client
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

  protected static AuthenticationInfoDTO authenticationInfo(WebTestClient client, String name) {
    return client
        .get()
        .uri("/api/authorization")
        .cookie(SESSION, session(client, name))
        .exchange()
        .expectStatus()
        .isOk()
        .returnResult(AuthenticationInfoDTO.class)
        .getResponseBody()
        .blockFirst();
  }
}
