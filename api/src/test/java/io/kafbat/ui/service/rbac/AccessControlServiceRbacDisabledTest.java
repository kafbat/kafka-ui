package io.kafbat.ui.service.rbac;

import static io.kafbat.ui.service.rbac.MockedRbacUtils.CONNECT_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.CONSUMER_GROUP_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.DEV_ROLE;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.PROD_CLUSTER;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.SCHEMA_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.TOPIC_NAME;
import static io.kafbat.ui.service.rbac.MockedRbacUtils.getAccessContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test cases for AccessControlService when RBAC is disabled.
 * Using PROD cluster and user DEV role for all tests.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AccessControlServiceRbacDisabledTest extends AbstractIntegrationTest {

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
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
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
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
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
  void filterViewableTopics() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      List<InternalTopic> topics = List.of(
          InternalTopic.builder()
              .name(TOPIC_NAME)
              .build()
      );
      Mono<List<InternalTopic>> filterTopicsMono = accessControlService.filterViewableTopics(topics, PROD_CLUSTER);
      StepVerifier.create(filterTopicsMono)
          .expectNextMatches(responseTopics -> responseTopics.stream().anyMatch(t -> t.getName().equals(TOPIC_NAME)))
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConsumerGroupAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConsumerGroupAccessible(CONSUMER_GROUP_NAME, PROD_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isSchemaAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isSchemaAccessible(SCHEMA_NAME, PROD_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible(CONNECT_NAME, PROD_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void isConnectAccessibleDto() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of(DEV_ROLE));
      ConnectDTO connectDto = new ConnectDTO()
          .name(CONNECT_NAME);
      Mono<Boolean> consumerGroupAccessibleMono =
          accessControlService.isConnectAccessible(connectDto, PROD_CLUSTER);
      StepVerifier.create(consumerGroupAccessibleMono)
          .expectNext(true)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void getRoles() {
    List<Role> roles = accessControlService.getRoles();
    assertThat(roles).isEmpty();
  }

}
