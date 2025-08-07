package io.kafbat.ui.service.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class KafkaPropertiesConstraintsValidator {
  private Integer replicationFactor;
  private Map<String, String> configs;

  public void validate() {
    minInSyncReplicasLessThanReplicationFactorValidation();
    compressionConfigValueValidation();
    compactionConfigValuesValidation();
    remoteStorageConfigValuesValidation();
    retentionAndDeletionTimeConfigurationBasedConstraintsValidation();
    retentionAndDeletionMemoryConfigurationBasedConstraintsValidation();
  }

  void minInSyncReplicasLessThanReplicationFactorValidation() {
    Integer minInSyncReplicas = configs.get("min.insync.replicas") != null
                                ? Integer.parseInt(configs.get("min.insync.replicas"))
                                : null;

    if (minInSyncReplicas != null && replicationFactor != null && minInSyncReplicas > replicationFactor) {
      throw new IllegalArgumentException(
          String.format("min.insync.replicas (%d) should be less than or equal to replication.factor (%d)",
              minInSyncReplicas, replicationFactor));
    }
  }

  void compressionConfigValueValidation() {
    String compressionType = configs.get("compression.type");
    if (configs.get("compression.zstd.level") != null && !"zstd".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.zstd.level (%s) should be set only when compression.type is zstd",
              configs.get("compression.zstd.level")));
    }
    if (configs.get("compression.lz4.level") != null && !"lz4".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.lz4.level (%s) should be set only when compression.type is lz4",
              configs.get("compression.lz4.level")));
    }
    if (configs.get("compression.gzip.level") != null && !"gzip".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.gzip.level (%s) should be set only when compression.type is gzip",
              configs.get("compression.gzip.level")));
    }
  }

  void compactionConfigValuesValidation() {
    String cleanupPolicy = configs.get("cleanup.policy");
    List<String> policies = new ArrayList<>();
    if (cleanupPolicy != null) {
      policies = Arrays.asList(cleanupPolicy.split(","));
    }
    if (configs.get("min.cleanable.dirty.ratio") != null && !policies.contains("compact")) {
      throw new IllegalArgumentException(
          String.format("min.cleanable.dirty.ratio (%s) should be set only when cleanup.policy is compact",
              configs.get("min.cleanable.dirty.ratio")));
    }
    if (configs.get("min.compaction.lag.ms") != null && !policies.contains("compact")) {
      throw new IllegalArgumentException(
          String.format("min.compaction.lag.ms (%s) should be set only when cleanup.policy is compact",
              configs.get("min.compaction.lag.ms")));
    }
    if (configs.get("max.compaction.lag.ms") != null && !policies.contains("compact")) {
      throw new IllegalArgumentException(
          String.format("max.compaction.lag.ms (%s) should be set only when cleanup.policy is compact",
              configs.get("max.compaction.lag.ms")));
    }
    if (configs.get("delete.retention.ms") != null && !policies.contains("compact")) {
      throw new IllegalArgumentException(
          String.format("delete.retention.ms (%s) should be set only when cleanup.policy is compact",
              configs.get("delete.retention.ms")));
    }

  }

  void remoteStorageConfigValuesValidation() {
    String remoteStorageEnabled = configs.get("remote.storage.enable");
    if (configs.get("local.retention.ms") != null && !"true".equals(remoteStorageEnabled)) {
      throw new IllegalArgumentException(
          String.format("local.retention.ms (%s) should be set only when remoteStorageEnabled is true",
              configs.get("local.retention.ms")));
    }
    if (configs.get("local.retention.bytes") != null && !"true".equals(remoteStorageEnabled)) {
      throw new IllegalArgumentException(
          String.format("local.retention.bytes (%s) should be set only when remoteStorageEnabled is true",
              configs.get("local.retention.bytes")));
    }
  }

  void retentionAndDeletionTimeConfigurationBasedConstraintsValidation() {
    List<String> keys = new ArrayList<>();
    List<Long> values = new ArrayList<>();
    if (!Objects.equals(configs.get("retention.ms"), "-1") && configs.get("retention.ms") != null) {
      keys.add("retention.ms");
      values.add(parseLong(configs.get("retention.ms")));
    }
    if (!Objects.equals(configs.get("local.retention.ms"), "-2") && configs.get("local.retention.ms") != null) {
      keys.add("local.retention.ms");
      values.add(parseLong(configs.get("local.retention.ms")));
    }
    if (configs.get("segment.ms") != null) {
      keys.add("segment.ms");
      values.add(parseLong(configs.get("segment.ms")));
    }

    for (int i = 0; i < values.size() - 1; i++) {
      Long current = values.get(i);
      Long next = values.get(i + 1);
      if (current != 0 && next != 0 && current < next) {
        throw new IllegalArgumentException(
            String.format("Invalid configuration: %s (%s) should be greater than or equal to %s (%s)",
                keys.get(i), configs.get(keys.get(i)),
                keys.get(i + 1), configs.get(keys.get(i + 1))));
      }
    }

  }

  void retentionAndDeletionMemoryConfigurationBasedConstraintsValidation() {
    List<String> keys = new ArrayList<>();
    List<Long> values = new ArrayList<>();
    if (!Objects.equals(configs.get("retention.bytes"), "-1") && configs.get("retention.bytes") != null) {
      keys.add("retention.bytes");
      values.add(parseLong(configs.get("retention.bytes")));
    }
    if (!Objects.equals(configs.get("local.retention.bytes"), "-2") && configs.get("local.retention.bytes") != null) {
      keys.add("local.retention.bytes");
      values.add(parseLong(configs.get("local.retention.bytes")));
    }
    if (configs.get("segment.bytes") != null) {
      keys.add("segment.bytes");
      values.add(parseLong(configs.get("segment.bytes")));
    }
    if (configs.get("max.message.bytes") != null) {
      keys.add("max.message.bytes");
      values.add(parseLong(configs.get("max.message.bytes")));
    }

    for (int i = 0; i < values.size() - 1; i++) {
      Long current = values.get(i);
      Long next = values.get(i + 1);
      if (current != 0 && next != 0 && current < next) {
        throw new IllegalArgumentException(
            String.format("Invalid configuration: %s (%s) should be greater than or equal to %s (%s)",
                keys.get(i), configs.get(keys.get(i)),
                keys.get(i + 1), configs.get(keys.get(i + 1)
                )));
      }
    }

  }

  private static long parseLong(String value) {
    try {
      return value != null ? Long.parseLong(value) : 0L;
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

}
