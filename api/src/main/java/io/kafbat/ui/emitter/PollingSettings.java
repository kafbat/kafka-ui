package io.kafbat.ui.emitter;

import io.kafbat.ui.config.ClustersProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class PollingSettings {

  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMillis(1_000);
  private static final long DEFAULT_MAX_BYTES_CONSUMED = 5L * 1024 * 1024;

  private final Duration pollTimeout;
  private final long maxBytesConsumed;
  private final Supplier<PollingThrottler> throttlerSupplier;

  public static PollingSettings create(ClustersProperties.Cluster cluster,
                                       ClustersProperties clustersProperties) {
    var pollingProps = Optional.ofNullable(clustersProperties.getPolling())
        .orElseGet(ClustersProperties.PollingProperties::new);

    var pollTimeout = pollingProps.getPollTimeoutMs() != null
        ? Duration.ofMillis(pollingProps.getPollTimeoutMs())
        : DEFAULT_POLL_TIMEOUT;

    return new PollingSettings(
        pollTimeout,
        Optional.ofNullable(pollingProps.getMaxBytesConsumed()).orElse(DEFAULT_MAX_BYTES_CONSUMED),
        PollingThrottler.throttlerSupplier(cluster)
    );
  }

  public static PollingSettings createDefault() {
    return new PollingSettings(
        DEFAULT_POLL_TIMEOUT,
        DEFAULT_MAX_BYTES_CONSUMED,
        PollingThrottler::noop
    );
  }

  public static PollingSettings createDefault(long maxBytesConsumed) {
    return new PollingSettings(
        DEFAULT_POLL_TIMEOUT,
        maxBytesConsumed,
        PollingThrottler::noop
    );
  }

  private PollingSettings(Duration pollTimeout,
                          long maxBytesConsumed,
                          Supplier<PollingThrottler> throttlerSupplier) {
    this.pollTimeout = pollTimeout;
    this.maxBytesConsumed = maxBytesConsumed;
    this.throttlerSupplier = throttlerSupplier;
  }

  public Duration getPollTimeout() {
    return pollTimeout;
  }

  public PollingThrottler getPollingThrottler() {
    return throttlerSupplier.get();
  }

  public long getMaxBytesConsumed() {
    return maxBytesConsumed;
  }
}
