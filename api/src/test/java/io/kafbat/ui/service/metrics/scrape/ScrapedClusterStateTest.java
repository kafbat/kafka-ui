package io.kafbat.ui.service.metrics.scrape;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScrapedClusterStateTest {

  @Test
  void emptyStateHasNonNullTopicIndex() throws Exception {
    try (ScrapedClusterState empty = ScrapedClusterState.empty()) {
      assertThat(empty.getTopicIndex()).isNotNull();
      assertThat(empty.getTopicIndex().find(null, null, false, null)).isEmpty();
      assertThat(empty.getTopicIndex().find("search", true, true, null)).isEmpty();
    }
  }
}
