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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for issues #76.
 * User is allowed to create topic based only on pattern "test-.*"
 */
@ActiveProfiles("topiccreation")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RbacTopicCreationTest extends AbstractIntegrationTest {

  public static final String ROLE_NAME = "dev_role";
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
        getRole()
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

  /**
   * Create a "test-" prefixed topic, allowed.
   */
  @Test
  void validateAccess() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .topicActions("test-topic", TopicAction.CREATE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  /**
   * Create a not "test-" prefixed topic, not allowed.
   */
  @Test
  void validateAccess_accessDenied() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ROLE_NAME));
      AccessContext context = AccessContext.builder()
          .cluster(CLUSTER_NAME)
          .topicActions("another-topic", TopicAction.CREATE)
          .build();
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  public static Role getRole() {
    Role role = new Role();
    role.setName(ROLE_NAME);
    role.setClusters(List.of(CLUSTER_NAME));
    Subject sub = new Subject();
    sub.setType("group");
    sub.setProvider(Provider.LDAP);
    sub.setValue("kafbat.group");
    role.setSubjects(List.of(sub));
    Permission testTopicsPermission = new Permission();
    testTopicsPermission.setResource(Resource.TOPIC.name());
    testTopicsPermission.setActions(List.of(
        TopicAction.VIEW.name(),
        TopicAction.CREATE.name(),
        TopicAction.EDIT.name(),
        TopicAction.DELETE.name(),
        TopicAction.MESSAGES_READ.name(),
        TopicAction.MESSAGES_PRODUCE.name()
    ));
    testTopicsPermission.setValue("test-.*");

    Permission notPrefixedTopicPermission = new Permission();
    notPrefixedTopicPermission.setResource(Resource.TOPIC.name());
    notPrefixedTopicPermission.setActions(List.of(
        TopicAction.VIEW.name(),
        TopicAction.MESSAGES_READ.name(),
        TopicAction.MESSAGES_PRODUCE.name()
    ));
    notPrefixedTopicPermission.setValue("^(?!test-).*");

    List<Permission> permissions = List.of(
        testTopicsPermission,
        notPrefixedTopicPermission
    );
    role.setPermissions(permissions);
    role.validate();
    return role;
  }

}
