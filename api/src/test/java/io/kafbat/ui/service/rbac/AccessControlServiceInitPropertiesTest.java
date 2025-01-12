package io.kafbat.ui.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.service.rbac.extractor.GoogleAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case for Properties initializer.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(initializers = AccessControlServiceInitPropertiesTest.PropertiesInitializer.class)
class AccessControlServiceInitPropertiesTest extends AbstractIntegrationTest {

  public static class PropertiesInitializer extends AbstractIntegrationTest.Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
      System.setProperty("rbac.roles[0].name", "memelords");
      System.setProperty("rbac.roles[0].clusters[0]", "local");

      System.setProperty("rbac.roles[0].subjects[0].provider", "oauth_google");
      System.setProperty("rbac.roles[0].subjects[0].type", "domain");
      System.setProperty("rbac.roles[0].subjects[0].value", "katbat.dev");

      System.setProperty("rbac.roles[0].subjects[1].provider", "oauth_google");
      System.setProperty("rbac.roles[0].subjects[1].type", "user");
      System.setProperty("rbac.roles[0].subjects[1].value", "name@kafbat.dev");

      System.setProperty("rbac.roles[0].permissions[0].resource", "applicationconfig");
      System.setProperty("rbac.roles[0].permissions[0].actions", "all");
    }
  }

  @Autowired
  AccessControlService accessControlService;

  @Test
  void rbacEnabled() {
    assertTrue(accessControlService.isRbacEnabled());

    Set<ProviderAuthorityExtractor> oauthExtractors = accessControlService.getOauthExtractors();
    assertThat(oauthExtractors)
        .hasSize(1)
        .anyMatch(ext -> ext instanceof GoogleAuthorityExtractor);
  }

}
