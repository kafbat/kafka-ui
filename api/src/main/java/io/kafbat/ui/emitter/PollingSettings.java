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

  /** Resolves per-application polling settings, including defaults and cluster throttling. */
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

  /** Creates polling settings with application defaults and no throttling. */
  public static PollingSettings createDefault() {
    return new PollingSettings(
        DEFAULT_POLL_TIMEOUT,
        DEFAULT_MAX_BYTES_CONSUMED,
        PollingThrottler::noop
    );
  }

  /** Creates default polling settings with a test- or caller-supplied byte limit. */
  public static PollingSettings createDefault(long maxBytesConsumed) {
    return new PollingSettings(
        DEFAULT_POLL_TIMEOUT,
        maxBytesConsumed,
        PollingThrottler::noop
    );
  }

  /** Stores resolved timeout, byte-limit, and throttling configuration. */
  private PollingSettings(Duration pollTimeout,
                          long maxBytesConsumed,
                          Supplier<PollingThrottler> throttlerSupplier) {
    this.pollTimeout = pollTimeout;
    this.maxBytesConsumed = maxBytesConsumed;
    this.throttlerSupplier = throttlerSupplier;
  }

  /** Returns the maximum duration to wait for each Kafka poll. */
  public Duration getPollTimeout() {
    return pollTimeout;
  }

  /** Creates the configured throttler for this polling operation. */
  public PollingThrottler getPollingThrottler() {
    return throttlerSupplier.get();
  }

  /** Returns the maximum serialized bytes admitted in one polling request. */
  public long getMaxBytesConsumed() {
    return maxBytesConsumed;
  }
}
