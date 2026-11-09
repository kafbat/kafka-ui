package io.kafbat.ui;

import static io.kafbat.ui.container.ActiveDirectoryContainer.DOMAIN;

import io.kafbat.ui.container.ActiveDirectoryContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

@DirtiesContext
@ContextConfiguration(initializers = {ActiveDirectoryLdapTest.Initializer.class})
public class ActiveDirectoryLdapTest extends AbstractActiveDirectoryIntegrationTest {
  private static final ActiveDirectoryContainer ACTIVE_DIRECTORY = new ActiveDirectoryContainer(false);

  @Autowired
  private WebTestClient webTestClient;

  @BeforeAll
  public static void setup() {
    ACTIVE_DIRECTORY.start();
  }

  @AfterAll
  public static void shutdown() {
    ACTIVE_DIRECTORY.stop();
  }

  @Test
  public void testUserPermissions() {
    checkUserPermissions(webTestClient);
  }

  @Test
  public void testEmptyPermissions() {
    checkEmptyPermissions(webTestClient);
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
      System.setProperty("spring.ldap.urls", ACTIVE_DIRECTORY.getLdapUrl());
      System.setProperty("oauth2.ldap.activeDirectory", "true");
      System.setProperty("oauth2.ldap.activeDirectory.domain", DOMAIN);
    }
  }
}
