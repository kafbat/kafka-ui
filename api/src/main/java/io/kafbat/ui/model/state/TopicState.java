package io.kafbat.ui.model.state;

import io.kafbat.ui.model.InternalLogDirStats;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;

public record TopicState(
    String name,
    TopicDescription description,
    List<ConfigEntry> configs,
    Map<Integer, Long> startOffsets,
    Map<Integer, Long> endOffsets,
    @Nullable InternalLogDirStats.SegmentStats segmentStats,
    @Nullable Map<Integer, InternalLogDirStats.SegmentStats> partitionsSegmentStats) {
}
