package io.kafbat.ui.service.rbac;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.rbac.AccessContext;
import java.util.List;
import io.kafbat.ui.model.rbac.Role;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

//@ContextConfiguration(initializers = {AccessControlServiceTest.PropertiesInitializer.class})
class AccessControlServiceTest extends AbstractIntegrationTest {

//  public static class PropertiesInitializer extends AbstractIntegrationTest.Initializer
//      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

//    @Override
//    public void initialize(ConfigurableApplicationContext applicationContext) {
//      System.setProperty("rbac.roles[0].name", "memelords");
//      System.setProperty("rbac.roles[0].clusters[0]", "local");
//
//      System.setProperty("rbac.roles[0].subjects[0].provider", "oauth_google");
//      System.setProperty("rbac.roles[0].subjects[0].type", "domain");
//      System.setProperty("rbac.roles[0].subjects[0].value", "katbat.dev");
//
//      System.setProperty("rbac.roles[0].subjects[1].provider", "oauth_google");
//      System.setProperty("rbac.roles[0].subjects[1].type", "user");
//      System.setProperty("rbac.roles[0].subjects[1].value", "name@kafbat.dev");
//
//      System.setProperty("rbac.roles[0].permissions[0].resource", "applicationconfig");
//      System.setProperty("rbac.roles[0].permissions[0].actions", "all");
//
//      super.initialize(applicationContext);
//    }
//  }

  @Autowired
  AccessControlService accessControlService;

  @Mock
  ReactiveSecurityContextHolder securityContextHolder;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  RbacUser user;

  @BeforeEach
  void setUp() {
    // Mock roles
    RoleBasedAccessControlProperties properties = mock();

    Role memeLordsRole = new Role();
    memeLordsRole.setClusters(List.of("local"));
    memeLordsRole.setName("memeLords");
    List<Role> roles = List.of(
        memeLordsRole
    );
    when(properties.getRoles()).thenReturn(roles);
    ReflectionTestUtils.setField(accessControlService, "properties", properties);

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
      when(user.groups()).thenReturn(List.of("memelords"));
      AccessContext.ResourceAccess mockedResource = mock(AccessContext.ResourceAccess.class);
      when(mockedResource.isAccessible(any())).thenReturn(true);
      var accessContext = new AccessContext("local", List.of(
          mockedResource
      ), "op", "params");

      Mono<Void> voidMono = accessControlService.validateAccess(accessContext);
      StepVerifier.create(voidMono)
          .expectComplete()
          .verify();
    });
  }

  @Test
  void validateAccess_deniedWrongGroup() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("otherGroup")); // wrong group
      AccessContext.ResourceAccess mockedResource = mock(AccessContext.ResourceAccess.class);
      when(mockedResource.isAccessible(any())).thenReturn(true);
      var accessContext = new AccessContext("local", List.of(
          mockedResource
      ), "op", "params");

      Mono<Void> voidMono = accessControlService.validateAccess(accessContext);
      StepVerifier.create(voidMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  @Test
  void validateAccess_deniedWrongCluster() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("memelords"));
      AccessContext.ResourceAccess mockedResource = mock(AccessContext.ResourceAccess.class);
      when(mockedResource.isAccessible(any())).thenReturn(true);
      var accessContext = new AccessContext("prod", // wrong cluster
          List.of(
              mockedResource
          ), "op", "params");

      Mono<Void> voidMono = accessControlService.validateAccess(accessContext);
      StepVerifier.create(voidMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

  @Test
  void validateAccess_deniedResourceNotAcessible() {
    withSecurityContext(() -> {
      when(user.groups()).thenReturn(List.of("memelords"));
      AccessContext.ResourceAccess mockedResource = mock(AccessContext.ResourceAccess.class);
      when(mockedResource.isAccessible(any())).thenReturn(false); // resource not acessible
      var accessContext = new AccessContext("local", List.of(
          mockedResource
      ), "op", "params");

      Mono<Void> voidMono = accessControlService.validateAccess(accessContext);
      StepVerifier.create(voidMono)
          .expectErrorMatches(e -> e instanceof AccessDeniedException)
          .verify();
    });
  }

}
