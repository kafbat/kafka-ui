package io.kafbat.ui.mapper;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.ActionDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesAuthOauth2ResourceServerDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesAuthOauth2ResourceServerJwtDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesAuthOauth2ResourceServerOpaquetokenDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesKafkaClustersInnerDTO;
import io.kafbat.ui.model.RbacPermissionDTO;
import io.kafbat.ui.model.rbac.Permission;
import io.kafbat.ui.util.DynamicConfigOperations;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Mapper(componentModel = "spring")
public interface DynamicConfigMapper {

  DynamicConfigOperations.PropertiesStructure fromDto(ApplicationConfigPropertiesDTO dto);

  @Mapping(target = "kafka.clusters[].metrics.store", ignore = true)
  ApplicationConfigPropertiesDTO toDto(DynamicConfigOperations.PropertiesStructure propertiesStructure);

  default String map(Resource resource) {
    return resource.getFilename();
  }

  @Mapping(source = "metrics.store", target = "metrics.store", ignore = true)
  ApplicationConfigPropertiesKafkaClustersInnerDTO map(ClustersProperties.Cluster cluster);

  default Permission map(RbacPermissionDTO perm) {
    Permission permission = new Permission();
    permission.setResource(perm.getResource().getValue());
    permission.setActions(perm.getActions().stream().map(ActionDTO::getValue).toList());
    permission.setValue(perm.getValue());
    return permission;
  }

  default OAuth2ResourceServerProperties map(ApplicationConfigPropertiesAuthOauth2ResourceServerDTO value) {
    if (value != null) {
      OAuth2ResourceServerProperties result = new OAuth2ResourceServerProperties();
      if (value.getJwt() != null) {
        OAuth2ResourceServerProperties.Jwt jwt = result.getJwt();

        ApplicationConfigPropertiesAuthOauth2ResourceServerJwtDTO source = value.getJwt();
        Optional.ofNullable(source.getJwsAlgorithms()).ifPresent(jwt::setJwsAlgorithms);
        Optional.ofNullable(source.getJwkSetUri()).ifPresent(jwt::setJwkSetUri);
        Optional.ofNullable(source.getIssuerUri()).ifPresent(jwt::setIssuerUri);
        Optional.ofNullable(source.getPublicKeyLocation())
            .map(this::mapResource)
            .ifPresent(jwt::setPublicKeyLocation);
        Optional.ofNullable(source.getAudiences()).ifPresent(jwt::setAudiences);
        Optional.ofNullable(source.getAuthoritiesClaimName()).ifPresent(jwt::setAuthoritiesClaimName);
        Optional.ofNullable(source.getAuthoritiesClaimDelimiter()).ifPresent(jwt::setAuthoritiesClaimDelimiter);
        Optional.ofNullable(source.getAuthorityPrefix()).ifPresent(jwt::setAuthorityPrefix);
        Optional.ofNullable(source.getPrincipalClaimName()).ifPresent(jwt::setPrincipalClaimName);
      }
      if (value.getOpaquetoken() != null) {
        OAuth2ResourceServerProperties.Opaquetoken opaquetoken = result.getOpaquetoken();
        ApplicationConfigPropertiesAuthOauth2ResourceServerOpaquetokenDTO source = value.getOpaquetoken();
        Optional.ofNullable(source.getClientId()).ifPresent(opaquetoken::setClientId);
        Optional.ofNullable(source.getClientSecret()).ifPresent(opaquetoken::setClientSecret);
        Optional.ofNullable(source.getIntrospectionUri()).ifPresent(opaquetoken::setIntrospectionUri);
      }
    }
    return null;
  }

  default Resource mapResource(String filename) {
    return new FileSystemResource(filename);
  }

  default ActionDTO stringToActionDto(String str) {
    return Optional.ofNullable(str)
        .map(s -> Enum.valueOf(ActionDTO.class, s.toUpperCase()))
        .orElseThrow();
  }
}
