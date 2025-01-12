package io.kafbat.ui.service.rbac;

import static io.kafbat.ui.service.rbac.MockedRbacUtils.ADMIN_ROLE;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.CONNECT_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.CONSUMER_GROUP_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.DEV_CLUSTER;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.DEV_ROLE;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.PROD_CLUSTER;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.SCHEMA_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.TOPIC_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.getAccessContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.Role;
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
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for AccessControlService when RBAC is enabled.
 */
class AccessControlServiceRbacEnabledTest extends AbstractIntegrationTest {

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
        MockedRbacUtils.getAdminRole(),
        MockedRbacUtils.getDevRole()
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
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE));
      AccessContext context = getAccessContext(PROD_CLUSTER, true);
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void validateAccess_deniedCluster() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      AccessContext context = getAccessContext(PROD_CLUSTER, true);
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  @Test
  void validateAccess_deniedResourceNotAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE));
      AccessContext context = getAccessContext(PROD_CLUSTER, false);
      Mono<Void> validateAccessMono = accessControlService.validateAccess(context);
      StepVerifier.create(validateAccessMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  @Test
  void isClusterAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(ADMIN_ROLE));
      ClusterDTO clusterDto = new ClusterDTO();
      clusterDto.setName(PROD_CLUSTER);
      Mono<Boolean> clusterAccessibleMono = accessControlService.isClusterAccessible(clusterDto);
      StepVerifier.create(clusterAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isClusterAccessible_deniedCluster() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ClusterDTO clusterDto = new ClusterDTO();
      clusterDto.setName(PROD_CLUSTER);
      Mono<Boolean> clusterAccessibleMono = accessControlService.isClusterAccessible(clusterDto);
      StepVerifier.create(clusterAccessibleMono)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void filterViewableTopics() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ClusterDTO clusterDto = new ClusterDTO();
      clusterDto.setName(DEV_CLUSTER);
      List<InternalTopic> topics = List.of(
          InternalTopic.builder()
              .name(TOPIC_NAME)
              .build()
      );
      Mono<List<InternalTopic>> filterTopicsMono = accessControlService.filterViewableTopics(topics, DEV_CLUSTER);
      StepVerifier.create(filterTopicsMono)
          .expectNextMatches(responseTopics -> responseTopics.stream().anyMatch(t -> t.getName().equals(TOPIC_NAME)))
          .expectComplete()
          .verify();
    });
  }

  @Test
  void filterViewableTopics_notAccessibleTopic() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ClusterDTO clusterDto = new ClusterDTO();
      clusterDto.setName(DEV_CLUSTER);
      List<InternalTopic> topics = List.of(
          InternalTopic.builder()
              .name("some other topic")
              .build()
      );
      Mono<List<InternalTopic>> filterTopicsMono = accessControlService.filterViewableTopics(topics, DEV_CLUSTER);
      StepVerifier.create(filterTopicsMono)
          .expectNextMatches(List::isEmpty)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConsumerGroupAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConsumerGroupAccessible(CONSUMER_GROUP_NAME, DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConsumerGroupAccessible_notAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConsumerGroupAccessible("SOME OTHER CONSUMER", DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isSchemaAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isSchemaAccessible(SCHEMA_NAME, DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isSchemaAccessible_notAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isSchemaAccessible("SOME OTHER SCHEMA", DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible(CONNECT_NAME, DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessible_notAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible("SOME OTHER CONNECT", DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessibleDto() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ConnectDTO connectDto = ConnectDTO.builder()
          .name(CONNECT_NAME)
          .build();
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible(connectDto, DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessibleDto_notAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ConnectDTO connectDto = ConnectDTO.builder()
          .name("SOME OTHER CONNECT")
          .build();
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible(connectDto, DEV_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(false)
          .expectComplete()
          .verify();
    });
  }

}
