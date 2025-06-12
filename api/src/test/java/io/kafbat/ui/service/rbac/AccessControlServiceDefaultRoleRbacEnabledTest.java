package io.kafbat.ui.service.rbac;

import static io.kafbat.ui.service.rbac.MockedRbacUtils.DEFAULT_ROLE;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.DEV_CLUSTER;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.PROD_CLUSTER;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.getAccessContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.ClustersStorage;
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
 * Test class for AccessControlService with default role and RBAC enabled.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AccessControlServiceDefaultRoleRbacEnabledTest extends AbstractIntegrationTest {

  @Autowired
  AccessControlService accessControlService;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  RbacUser user;

  @Mock
  DefaultRole defaultRole;

  @Mock
  ClustersStorage clustersStorage;

  @BeforeEach
  void setUp() {

    RoleBasedAccessControlProperties properties = mock();
    defaultRole = MockedRbacUtils.getDefaultRole();
    when(properties.getDefaultRole()).thenReturn(defaultRole);
    when(properties.getRoles()).thenReturn(List.of()); // Return empty list for roles


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
      when(user.groups()).thenReturn(List.of(DEFAULT_ROLE));
      AccessContext context = getAccessContext(PROD_CLUSTER, true);
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isClusterAccessible() {
    withSecurityContext(() -> {
      ClusterDTO clusterDto = new ClusterDTO();
      clusterDto.setName(PROD_CLUSTER);
      Mono<Boolean> clusterAccessibleMono = accessControlService.isClusterAccessible(clusterDto);
      StepVerifier.create(clusterAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }
}
