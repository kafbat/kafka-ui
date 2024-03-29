package io.kafbat.ui.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class InternalBrokerDiskUsage {
  private final long segmentCount;
  private final long segmentSize;
}
