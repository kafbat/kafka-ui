package io.kafbat.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.kafbat.ui.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationInfoServiceTest extends AbstractIntegrationTest {
  @Autowired
  private ApplicationInfoService service;

  @Test
  void testCustomGithubReleaseInfoTimeout() {
    assertEquals(100, service.githubReleaseInfo().getGithubApiMaxWaitTime());
  }
}
