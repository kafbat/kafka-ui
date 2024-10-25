package io.kafbat.ui.controller;

import io.kafbat.ui.api.AuthorizationApi;
import io.kafbat.ui.model.ActionDTO;
import io.kafbat.ui.model.AuthenticationInfoDTO;
import io.kafbat.ui.model.ResourceTypeDTO;
import io.kafbat.ui.model.UserInfoDTO;
import io.kafbat.ui.model.UserPermissionDTO;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController implements AuthorizationApi {

  private final AccessControlService accessControlService;

  public Mono<ResponseEntity<AuthenticationInfoDTO>> getUserAuthInfo(ServerWebExchange exchange) {
    Mono<List<UserPermissionDTO>> permissions = AccessControlService.getUser()
        .map(user -> accessControlService.getRoles()
            .stream()
            .filter(role -> user.groups().contains(role.getName()))
            .map(role -> mapPermissions(role.getPermissions(), role.getClusters()))
            .flatMap(Collection::stream)
            .toList()
        )
        .switchIfEmpty(Mono.just(Collections.emptyList()));

    Mono<String> userName = ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Principal::getName);

    return userName
        .zipWith(permissions)
        .map(data -> {
          var dto = new AuthenticationInfoDTO(accessControlService.isRbacEnabled());
          dto.setUserInfo(new UserInfoDTO(data.getT1(), data.getT2()));
          return dto;
        })
        .switchIfEmpty(Mono.just(new AuthenticationInfoDTO(accessControlService.isRbacEnabled())))
        .map(ResponseEntity::ok);
  }

  private List<UserPermissionDTO> mapPermissions(List<Permission> permissions, List<String> clusters) {
    return permissions
        .stream()
        .map(permission -> {
          UserPermissionDTO dto = new UserPermissionDTO();
          dto.setClusters(clusters);
          dto.setResource(ResourceTypeDTO.fromValue(permission.getResource().toString().toUpperCase()));
          dto.setValue(permission.getValue());
          dto.setActions(permission.getParsedActions()
              .stream()
              .map(p -> p.name().toUpperCase())
              .map(this::mapAction)
              .filter(Objects::nonNull)
              .toList());
          return dto;
        })
        .toList();
  }

  @Nullable
  private ActionDTO mapAction(String name) {
    try {
      return ActionDTO.fromValue(name);
    } catch (IllegalArgumentException e) {
      log.warn("Unknown Action [{}], skipping", name);
      return null;
    }
  }

}
