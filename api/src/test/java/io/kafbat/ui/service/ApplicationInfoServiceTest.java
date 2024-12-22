package io.kafbat.ui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.kafbat.ui.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationInfoServiceTest extends AbstractIntegrationTest {
  @Autowired
  private ApplicationInfoService service;

  @Test
  public void testCustomGithubReleaseInfoTimeout() {
    assertEquals(100, service.githubReleaseInfo().getGithubApiMaxWaitTime());
  }
}
