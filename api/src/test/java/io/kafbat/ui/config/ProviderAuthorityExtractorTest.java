package io.kafbat.ui.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class ProviderAuthorityExtractorTest {


  private final AccessControlService accessControlService = new AccessControlServiceMock().getMock();
  Yaml yaml;
  ProviderAuthorityExtractor extractor;

  @BeforeEach
  void setUp() {
    yaml = new Yaml();
    yaml.setBeanAccess(BeanAccess.FIELD);
    extractor = new OauthAuthorityExtractor();

    InputStream rolesFile = this.getClass()
        .getClassLoader()
        .getResourceAsStream("roles_definition.yaml");

    Role[] roleArray = yaml.loadAs(rolesFile, Role[].class);
    when(accessControlService.getRoles()).thenReturn(List.of(roleArray));

  }

  @SneakyThrows
  @Test
  void extractAuthoritiesFromRegex() {

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("role_definition", Set.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", "john@kafka.com"),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertEquals(Set.of("viewer", "admin"), roles);

  }

}
