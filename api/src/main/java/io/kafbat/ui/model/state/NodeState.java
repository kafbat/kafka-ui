package io.kafbat.ui.model.state;

import io.kafbat.ui.model.InternalLogDirStats;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.Node;

public record NodeState(int id,
                        Node node,
                        @Nullable InternalLogDirStats.SegmentStats segmentStats,
                        @Nullable InternalLogDirStats.LogDirSpaceStats logDirSpaceStats) {
}
