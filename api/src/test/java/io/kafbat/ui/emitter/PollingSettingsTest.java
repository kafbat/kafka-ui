package io.kafbat.ui.emitter;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PollingSettingsTest {

  @Test
  void usesConfiguredMaxMessagesToScanPerPoll() {
    var settings = createPollingSettings(321);

    assertThat(settings.getMaxMessagesToScanPerPoll()).isEqualTo(321);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -500})
  void fallsBackToDefaultWhenConfiguredMaxMessagesToScanPerPollIsNonPositive(int configuredValue) {
    int expectedDefault = PollingSettings.createDefault().getMaxMessagesToScanPerPoll();

    var settings = createPollingSettings(configuredValue);

    assertThat(settings.getMaxMessagesToScanPerPoll()).isEqualTo(expectedDefault);
  }

  @Test
  void usesDefaultWhenMaxMessagesToScanPerPollIsNotConfigured() {
    int expectedDefault = PollingSettings.createDefault().getMaxMessagesToScanPerPoll();

    var settings = createPollingSettings(null);

    assertThat(settings.getMaxMessagesToScanPerPoll()).isEqualTo(expectedDefault);
  }

  private PollingSettings createPollingSettings(Integer maxMessagesToScanPerPoll) {
    var cluster = new ClustersProperties.Cluster();
    cluster.setName("test-cluster");

    var rootProps = new ClustersProperties();
    var pollingProps = new ClustersProperties.PollingProperties();
    pollingProps.setMaxMessagesToScanPerPoll(maxMessagesToScanPerPoll);
    rootProps.setPolling(pollingProps);

    return PollingSettings.create(cluster, rootProps);
  }
}
