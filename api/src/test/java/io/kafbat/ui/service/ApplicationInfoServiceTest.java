package io.kafbat.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.util.DynamicConfigOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationInfoServiceTest extends AbstractIntegrationTest {
  @Autowired
  private ApplicationInfoService service;

  @Autowired
  private DynamicConfigOperations dynamicConfigOperations;

  @Test
  void testCustomGithubReleaseInfoTimeout() {
    assertEquals(100, service.githubReleaseInfo().getGithubApiMaxWaitTime());
  }

  @Test
  void testDisabledReleaseInfo() {
    var service2 = new ApplicationInfoService(
        dynamicConfigOperations,
        null,
        null,
        null,
        false,
        101
    );

    assertNull(service2.githubReleaseInfo(), "unexpected GitHub release info when disabled");
    var appInfo = service2.getApplicationInfo();
    assertNotNull(appInfo, "application info must not be NULL");
    assertNull(appInfo.getLatestRelease(), "latest release should be NULL when disabled");
    assertNotNull(appInfo.getBuild(), "build info must not be NULL");
    assertNotNull(appInfo.getEnabledFeatures(), "enabled features must not be NULL");
  }

}
