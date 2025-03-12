package io.kafbat.ui;

import static io.kafbat.ui.AbstractActiveDirectoryIntegrationTest.authenticationInfo;
import static io.kafbat.ui.AbstractIntegrationTest.LOCAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.UserPermissionDTO;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@ActiveProfiles("rbac-login-form")
@AutoConfigureWebTestClient(timeout = "60000")
public class BasicAuthIntegrationTest {
  @Autowired
  private WebTestClient client;

  @Test
  void testUserPermissions() {
    AuthenticationInfoDTO info = authenticationInfo(client, "admin", "pass");

    assertNotNull(info);
    assertTrue(info.getRbacEnabled());

    List<UserPermissionDTO> permissions = info.getUserInfo().getPermissions();

    assertEquals(1, permissions.size());

    UserPermissionDTO permission = permissions.getFirst();
    Set<TopicAction> actions = permission.getActions().stream()
        .map(dto -> TopicAction.valueOf(dto.getValue()))
        .collect(Collectors.toSet());

    assertTrue(permission.getClusters().contains(LOCAL)
        && permission.getResource() == ResourceTypeDTO.TOPIC
        && actions.equals(Set.of(TopicAction.values())));
  }
}
