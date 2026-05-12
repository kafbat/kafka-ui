package io.kafbat.ui.service.rbac;

import io.kafbat.ui.config.auth.AuthenticatedUser;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.ClusterDTO;
import io.kafbat.ui.model.ConnectDTO;
import io.kafbat.ui.model.InternalTopic;
import io.kafbat.ui.model.rbac.AccessContext;
import io.kafbat.ui.model.rbac.DefaultRole;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.model.rbac.Resource;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.model.rbac.Subject;
import io.kafbat.ui.model.rbac.permission.ConnectAction;
import io.kafbat.ui.model.rbac.permission.ConnectorAction;
import io.kafbat.ui.model.rbac.permission.ConsumerGroupAction;
import io.kafbat.ui.model.rbac.permission.SchemaAction;
import io.kafbat.ui.model.rbac.permission.TopicAction;
import io.kafbat.ui.service.rbac.extractor.CognitoAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GithubAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GoogleAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RoleBasedAccessControlProperties.class)
@Slf4j
public class AccessControlService {

  private static final String ACCESS_DENIED = "Access denied";

  @Nullable
  private final InMemoryReactiveClientRegistrationRepository clientRegistrationRepository;
  private final RoleBasedAccessControlProperties properties;
  private final Environment environment;

  @Getter
  private boolean rbacEnabled = false;
  @Getter
  private Set<ProviderAuthorityExtractor> oauthExtractors = Collections.emptySet();


  @PostConstruct
  public void init() {
    if (CollectionUtils.isEmpty(properties.getRoles()) && properties.getDefaultRole() == null) {
      log.trace("No roles provided, disabling RBAC");
      return;
    }
    rbacEnabled = true;

    this.oauthExtractors = properties.getRoles()
        .stream()
        .map(role -> role.getSubjects()
            .stream()
            .map(Subject::getProvider)
            .distinct()
            .map(provider -> switch (provider) {
                  case OAUTH_COGNITO -> new CognitoAuthorityExtractor();
                  case OAUTH_GOOGLE -> new GoogleAuthorityExtractor();
                  case OAUTH_GITHUB -> new GithubAuthorityExtractor();
                  case OAUTH -> new OauthAuthorityExtractor();
                  default -> null;
                }
            ).filter(Objects::nonNull)
            .collect(Collectors.toSet()))
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

    boolean hasRolesConfigured = !properties.getRoles().isEmpty() || properties.getDefaultRole() != null;
    if (hasRolesConfigured
        && "oauth2".equalsIgnoreCase(environment.getProperty("auth.type"))
        && (clientRegistrationRepository == null || !clientRegistrationRepository.iterator().hasNext())) {
      log.error("Roles are configured but no authentication methods are present. Authentication might fail.");
    }
  }

  public Mono<Void> validateAccess(AccessContext context) {
    return isAccessible(context)
        .flatMap(allowed -> allowed ? Mono.empty() : Mono.error(new AccessDeniedException(ACCESS_DENIED)))
        .then();
  }

  private Mono<Boolean> isAccessible(AccessContext context) {
    if (!rbacEnabled) {
      return Mono.just(true);
    }
    return getUser().map(user -> isAccessible(user, context));
  }

  private boolean isAccessible(AuthenticatedUser user, AccessContext context) {
    if (context.cluster() != null && !isClusterAccessible(context.cluster(), user)) {
      return false;
    }
    return context.isAccessible(getUserPermissions(user, context.cluster()));
  }

  private List<Permission> getUserPermissions(AuthenticatedUser user, @Nullable String clusterName) {
    List<Role> filteredRoles = properties.getRoles()
            .stream()
            .filter(filterRole(user))
            .filter(role -> clusterName == null || role.getClusters().stream().anyMatch(clusterName::equalsIgnoreCase))
            .toList();

    // if no roles are found, check if default role is set
    if (filteredRoles.isEmpty() && properties.getDefaultRole() != null) {
      return properties.getDefaultRole().getPermissions();
    }

    return filteredRoles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .toList();
  }

  public static Mono<AuthenticatedUser> getUser() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(authentication -> authentication.getPrincipal() instanceof RbacUser)
        .map(authentication -> ((RbacUser) authentication.getPrincipal()))
        .map(user -> new AuthenticatedUser(user.name(), user.groups()));
  }

  private boolean isClusterAccessible(String clusterName, AuthenticatedUser user) {
    Assert.isTrue(StringUtils.isNotEmpty(clusterName), "cluster value is empty");
    boolean isAccessible = properties.getRoles()
        .stream()
        .filter(filterRole(user))
        .anyMatch(role -> role.getClusters().stream().anyMatch(clusterName::equalsIgnoreCase));
    
    return isAccessible || properties.getDefaultRole() != null;
  }

  public Mono<Boolean> isClusterAccessible(ClusterDTO cluster) {
    if (!rbacEnabled) {
      return Mono.just(true);
    }
    return getUser().map(u -> isClusterAccessible(cluster.getName(), u));
  }

  public Mono<List<InternalTopic>> filterViewableTopics(List<InternalTopic> topics, String clusterName) {
    if (!rbacEnabled) {
      return Mono.just(topics);
    }
    return getUser()
        .map(user -> topics.stream()
            .filter(topic ->
                isAccessible(
                    user,
                    AccessContext.builder()
                        .cluster(clusterName)
                        .topicActions(topic.getName(), TopicAction.VIEW)
                        .build()
                )
            ).toList());
  }

  public Mono<Boolean> isConsumerGroupAccessible(String groupId, String clusterName) {
    return isAccessible(
        AccessContext.builder()
            .cluster(clusterName)
            .consumerGroupActions(groupId, ConsumerGroupAction.VIEW)
            .build()
    );
  }

  public Mono<Boolean> isSchemaAccessible(String schema, String clusterName) {
    return isAccessible(
        AccessContext.builder()
            .cluster(clusterName)
            .schemaActions(schema, SchemaAction.VIEW)
            .build()
    );
  }

  public Mono<Boolean> isConnectAccessible(ConnectDTO dto, String clusterName) {
    return isConnectAccessible(dto.getName(), clusterName);
  }

  public Mono<Boolean> isConnectAccessible(String connectName, String clusterName) {
    if (!rbacEnabled) {
      return Mono.just(true);
    }
    return getUser().map(user -> {
      List<Permission> permissions = getUserPermissions(user, clusterName);
      // Check direct connect VIEW permission
      boolean hasConnectPermission = AccessContext.builder()
          .cluster(clusterName)
          .connectActions(connectName, ConnectAction.VIEW)
          .build()
          .isAccessible(permissions);
      if (hasConnectPermission) {
        return true;
      }
      // Also show connect if user has any connector VIEW permission for it
      return permissions.stream()
          .filter(p -> p.getResource() == Resource.CONNECTOR)
          .filter(p -> p.getParsedActions().contains(ConnectorAction.VIEW))
          .anyMatch(p -> connectorPermissionMatchesConnect(p.getValue(), connectName));
    });
  }

  public Mono<Boolean> isConnectorAccessible(String connectName, String connectorName, String clusterName) {
    return isAccessible(
        AccessContext.builder()
            .cluster(clusterName)
            .connectorActions(connectName, connectorName, ConnectorAction.VIEW)
            .build()
    );
  }

  public List<Role> getRoles() {
    if (!rbacEnabled) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(properties.getRoles());
  }

  public DefaultRole getDefaultRole() {
    return properties.getDefaultRole();
  }

  private Predicate<Role> filterRole(AuthenticatedUser user) {
    return role -> user.groups().contains(role.getName());
  }

  /**
   * Checks if a connector permission value matches a given connect name.
   * Connector permission values are in format "connectPattern/connectorPattern".
   * This extracts the connect pattern and checks if connectName matches it.
   */
  private boolean connectorPermissionMatchesConnect(String permissionValue, String connectName) {
    if (permissionValue == null || !permissionValue.contains("/")) {
      return false;
    }
    String connectPattern = permissionValue.substring(0, permissionValue.indexOf('/'));
    return Pattern.compile(connectPattern).matcher(connectName).matches();
  }

}
