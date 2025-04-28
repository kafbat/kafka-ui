package io.kafbat.ui.service.rbac;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Resource;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for issue #260.
 * The role has permissions to delete messages from any topic
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacEditTopicTest extends AbstractIntegrationTest {

  public static final String ROLE_NAME = "role-name-ro";
  public static final String CLUSTER_NAME = "dev";

  @Autowired
  AccessControlService accessControlService;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  RbacUser user;

  @BeforeEach
  void setUp() {
    // Mock roles
    List<Role> roles = List.of(
        getEditTopicTestRole()
    );
    RoleBasedAccessControlProperties properties = mock();
    when(properties.getRoles()).thenReturn(roles);

    ReflectionTestUtils.setField(accessControlService, "properties", properties);
    ReflectionTestUtils.setField(accessControlService, "rbacEnabled", true);

    // Mock security context
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(user);
  }

  public void withSecurityContext(Runnable runnable) {
    try (MockedStatic<ReactiveSecurityContextHolder> ctxHolder = Mockito.mockStatic(
        ReactiveSecurityContextHolder.class)) {
      // Mock static method to get security context
      ctxHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
      runnable.run();
    }
  }

  @Test
  void validateAccess() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .topicActions("inventorytopic", TopicAction.MESSAGES_DELETE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void validateAccess_deniedRole() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("anotherRole"));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .topicActions("inventorytopic", TopicAction.MESSAGES_DELETE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  public static Role getEditTopicTestRole() {
    Role role = new Role();
    role.setName(ROLE_NAME);
    role.setClusters(List.of(CLUSTER_NAME));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission topicPermissionTestPrefix = new Permission();
    topicPermissionTestPrefix.setResource(Resource.TOPIC.name());
    topicPermissionTestPrefix.setActions(List.of(
        TopicAction.VIEW.name(),
        TopicAction.MESSAGES_READ.name(),
        TopicAction.MESSAGES_DELETE.name()
    ));
    topicPermissionTestPrefix.setValue(".*");

    List<Permission> permissions = List.of(
        topicPermissionTestPrefix
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

}
