package io.kafbat.ui.emitter;

import io.kafbat.ui.config.ClustersProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class PollingSettings {

  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMillis(1_000);
  private static final int DEFAULT_MAX_MESSAGES_TO_SCAN_PER_POLL = 500;

  private final Duration pollTimeout;
  private final int maxMessagesToScanPerPoll;
  private final Supplier<PollingThrottler> throttlerSupplier;

  public static PollingSettings create(ClustersProperties.Cluster cluster,
                                       ClustersProperties clustersProperties) {
    var pollingProps = Optional.ofNullable(clustersProperties.getPolling())
        .orElseGet(ClustersProperties.PollingProperties::new);

    var pollTimeout = pollingProps.getPollTimeoutMs() != null
        ? Duration.ofMillis(pollingProps.getPollTimeoutMs())
        : DEFAULT_POLL_TIMEOUT;
    var maxMessagesToScanPerPoll = Optional.ofNullable(pollingProps.getMaxMessagesToScanPerPoll())
        .filter(v -> v > 0)
        .orElse(DEFAULT_MAX_MESSAGES_TO_SCAN_PER_POLL);

    return new PollingSettings(
        pollTimeout,
        maxMessagesToScanPerPoll,
        PollingThrottler.throttlerSupplier(cluster)
    );
  }

  public static PollingSettings createDefault() {
    return new PollingSettings(
        DEFAULT_POLL_TIMEOUT,
        DEFAULT_MAX_MESSAGES_TO_SCAN_PER_POLL,
        PollingThrottler::noop
    );
  }

  private PollingSettings(Duration pollTimeout,
                          int maxMessagesToScanPerPoll,
                          Supplier<PollingThrottler> throttlerSupplier) {
    this.pollTimeout = pollTimeout;
    this.maxMessagesToScanPerPoll = maxMessagesToScanPerPoll;
    this.throttlerSupplier = throttlerSupplier;
  }

  public Duration getPollTimeout() {
    return pollTimeout;
  }

  public PollingThrottler getPollingThrottler() {
    return throttlerSupplier.get();
  }

  public int getMaxMessagesToScanPerPoll() {
    return maxMessagesToScanPerPoll;
  }
}
