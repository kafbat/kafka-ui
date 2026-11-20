package io.kafbat.ui.model.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.auth.RbacLdapUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.controller.AuthorizationController;
import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.UserPermissionDTO;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.model.rbac.provider.Provider;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class RoleTest {

  @Test
  void clusterMatchesSupportsLiteralWildcardAndRegexCaseInsensitively() {
    Role wildcardRole = createRole("admin", List.of("*"));
    wildcardRole.validate();

    assertThat(wildcardRole.clusterMatches("prod")).isTrue();
    assertThat(wildcardRole.clusterMatches("dev-cluster-01")).isTrue();
    assertThat(wildcardRole.clusterMatches("")).isFalse();
    assertThat(wildcardRole.clusterMatches(null)).isFalse();

    Role regexRole = createRole("prod-ops", List.of("prod-.*", "staging"));
    regexRole.validate();

    assertThat(regexRole.clusterMatches("prod-eu")).isTrue();
    assertThat(regexRole.clusterMatches("PROD-US")).isTrue();
    assertThat(regexRole.clusterMatches("STAGING")).isTrue();
    assertThat(regexRole.clusterMatches("dev-eu")).isFalse();
  }

  @Test
  void authorizationControllerResolvesMatchingClusterNamesForWildcardAndRegexRoles() {
    Role prodRegexRole = createRole("prod-ops", List.of("prod-.*"));
    RoleBasedAccessControlProperties rbacProps = new RoleBasedAccessControlProperties();
    rbacProps.setRoles(List.of(prodRegexRole));

    AccessControlService acs = new AccessControlService(null, rbacProps, new StandardEnvironment());
    acs.init();

    ClustersStorage clustersStorage = mock(ClustersStorage.class);
    when(clustersStorage.getKafkaClusters()).thenReturn(List.of(
        KafkaCluster.builder().name("prod-eu").build(),
        KafkaCluster.builder().name("prod-us").build(),
        KafkaCluster.builder().name("dev-eu").build()
    ));

    AuthorizationController controller = new AuthorizationController(acs, clustersStorage);
    RbacLdapUser principal = new RbacLdapUser(
        User.withUsername("alice").password("n/a").authorities("prod-ops").build()
    );
    Authentication auth = new UsernamePasswordAuthenticationToken(
        principal, "n/a", principal.getAuthorities()
    );

    ResponseEntity<AuthenticationInfoDTO> response = controller.getUserAuthInfo(null)
        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        .block();

    assertThat(response).isNotNull();
    assertThat(response.getBody()).isNotNull();
    List<UserPermissionDTO> permissions = response.getBody().getUserInfo().getPermissions();
    assertThat(permissions).hasSize(1);
    assertThat(permissions.getFirst().getClusters()).containsExactly("prod-eu", "prod-us");
  }

  private static Role createRole(String name, List<String> clusters) {
    Subject subject = new Subject();
    subject.setProvider(Provider.LDAP);
    subject.setType("group");
    subject.setValue("kafbat.group");

    Permission permission = new Permission();
    permission.setResource(Resource.TOPIC.name());
    permission.setValue(".*");
    permission.setActions(List.of(TopicAction.VIEW.name()));

    Role role = new Role();
    role.setName(name);
    role.setClusters(clusters);
    role.setSubjects(List.of(subject));
    role.setPermissions(List.of(permission));
    return role;
  }
}
