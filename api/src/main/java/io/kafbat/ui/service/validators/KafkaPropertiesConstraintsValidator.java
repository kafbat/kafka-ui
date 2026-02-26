package io.kafbat.ui.service.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@AllArgsConstructor
public class KafkaPropertiesConstraintsValidator {
  private Integer replicationFactor;
  private Map<String, String> configs;

  private static final String COMPACT = "compact";
  private static final String LOCAL_RETENTION_MS = "local.retention.ms";
  private static final String LOCAL_RETENTION_BYTES = "local.retention.bytes";
  private static final String RETENTION_MS = "retention.ms";
  private static final String SEGMENT_MS = "segment.ms";
  private static final String RETENTION_BYTES = "retention.bytes";
  private static final String SEGMENT_BYTES = "segment.bytes";
  private static final String MAX_MESSAGE_BYTES = "max.message.bytes";
  private static final String COMPRESSION_ZSTD_LEVEL = "compression.zstd.level";
  private static final String COMPRESSION_LZ4_LEVEL = "compression.lz4.level";
  private static final String COMPRESSION_GZIP_LEVEL = "compression.gzip.level";
  private static final String MIN_CLEANABLE_DIRTY_RATIO = "min.cleanable.dirty.ratio";
  private static final String MIN_COMPACTION_LAG_MS = "min.compaction.lag.ms";
  private static final String MAX_COMPACTION_LAG_MS = "max.compaction.lag.ms";
  private static final String DELETE_RETENTION_MS = "delete.retention.ms";

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
    if (configs.get(COMPRESSION_ZSTD_LEVEL) != null && !Objects.equals(configs.get(COMPRESSION_ZSTD_LEVEL), "3")
        && !"zstd".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.zstd.level (%s) should be set only when compression.type is zstd",
              configs.get(COMPRESSION_ZSTD_LEVEL)));
    }
    if (configs.get(COMPRESSION_LZ4_LEVEL) != null && !Objects.equals(configs.get(COMPRESSION_LZ4_LEVEL), "9")
        && !"lz4".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.lz4.level (%s) should be set only when compression.type is lz4",
              configs.get(COMPRESSION_LZ4_LEVEL)));
    }
    if (configs.get(COMPRESSION_GZIP_LEVEL) != null && !Objects.equals(configs.get(COMPRESSION_GZIP_LEVEL), "-1")
        && !"gzip".equals(compressionType)) {
      throw new IllegalArgumentException(
          String.format("compression.gzip.level (%s) should be set only when compression.type is gzip",
              configs.get(COMPRESSION_GZIP_LEVEL)));
    }
  }

  void compactionConfigValuesValidation() {
    String cleanupPolicy = configs.get("cleanup.policy");
    List<String> policies = new ArrayList<>();
    if (cleanupPolicy != null) {
      policies = Arrays.asList(cleanupPolicy.split(","));
    }
    if (configs.get(MIN_CLEANABLE_DIRTY_RATIO) != null
        && !Objects.equals(configs.get(MIN_CLEANABLE_DIRTY_RATIO), "0.5")
        && !policies.contains(COMPACT)) {
      throw new IllegalArgumentException(
          String.format("min.cleanable.dirty.ratio (%s) should be set only when cleanup.policy is compact",
              configs.get(MIN_CLEANABLE_DIRTY_RATIO)));
    }
    if (configs.get(MIN_COMPACTION_LAG_MS) != null
        && !Objects.equals(configs.get(MIN_COMPACTION_LAG_MS), "0")
        && !policies.contains(COMPACT)) {
      throw new IllegalArgumentException(
          String.format("min.compaction.lag.ms (%s) should be set only when cleanup.policy is compact",
              configs.get(MIN_COMPACTION_LAG_MS)));
    }
    if (configs.get(MAX_COMPACTION_LAG_MS) != null
        && !Objects.equals(configs.get(MAX_COMPACTION_LAG_MS), "9223372036854775807")
        && !policies.contains(COMPACT)) {
      throw new IllegalArgumentException(
          String.format("max.compaction.lag.ms (%s) should be set only when cleanup.policy is compact",
              configs.get(MAX_COMPACTION_LAG_MS)));
    }
    if (configs.get(DELETE_RETENTION_MS) != null
        && !Objects.equals(configs.get(DELETE_RETENTION_MS), "86400000")
        && !policies.contains(COMPACT)) {
      throw new IllegalArgumentException(
          String.format("delete.retention.ms (%s) should be set only when cleanup.policy is compact",
              configs.get(DELETE_RETENTION_MS)));
    }

  }

  void remoteStorageConfigValuesValidation() {
    String remoteStorageEnabled = configs.get("remote.storage.enable");
    if (configs.get(LOCAL_RETENTION_MS) != null && !Objects.equals(configs.get(LOCAL_RETENTION_MS), "-2")
        && !"true".equals(remoteStorageEnabled)) {
      throw new IllegalArgumentException(
          String.format("local.retention.ms (%s) should be set only when remoteStorageEnabled is true",
              configs.get(LOCAL_RETENTION_MS)));
    }
    if (configs.get(LOCAL_RETENTION_BYTES) != null && !Objects.equals(configs.get(LOCAL_RETENTION_BYTES), "-2")
        && !"true".equals(remoteStorageEnabled)) {
      throw new IllegalArgumentException(
          String.format("local.retention.bytes (%s) should be set only when remoteStorageEnabled is true",
              configs.get(LOCAL_RETENTION_BYTES)));
    }
  }

  void retentionAndDeletionTimeConfigurationBasedConstraintsValidation() {
    List<String> keys = new ArrayList<>();
    List<Long> values = new ArrayList<>();
    if (!Objects.equals(configs.get(RETENTION_MS), "-1")) {
      keys.add(RETENTION_MS);
      addRetentionMsToValues(values);
    }
    if (!Objects.equals(configs.get(LOCAL_RETENTION_MS), "-2") && configs.get(LOCAL_RETENTION_MS) != null) {
      keys.add(LOCAL_RETENTION_MS);
      values.add(parseLong(configs.get(LOCAL_RETENTION_MS)));
    }
    keys.add(SEGMENT_MS);
    if (configs.get(SEGMENT_MS) != null) {
      values.add(parseLong(configs.get(SEGMENT_MS)));
    } else {
      values.add(604800000L);
    }

    for (int i = 0; i < values.size() - 1; i++) {
      Long current = values.get(i);
      Long next = values.get(i + 1);
      if (current != 0 && next != 0 && current < next) {
        throw new IllegalArgumentException(
            String.format("Invalid configuration: %s (%s) should be greater than or equal to %s (%s)",
                keys.get(i), current,
                keys.get(i + 1), next));
      }
    }

  }

  void addRetentionMsToValues(List<Long> values) {
    if (configs.get(RETENTION_MS) != null) {
      values.add(parseLong(configs.get(RETENTION_MS)));
    } else {
      values.add(604800000L);
    }
  }

  void retentionAndDeletionMemoryConfigurationBasedConstraintsValidation() {

    List<String> keys = new ArrayList<>();
    List<Long> values = new ArrayList<>();
    if (!Objects.equals(configs.get(RETENTION_BYTES), "-1") && configs.get(RETENTION_BYTES) != null) {
      keys.add(RETENTION_BYTES);
      values.add(parseLong(configs.get(RETENTION_BYTES)));
    }
    if (!Objects.equals(configs.get(LOCAL_RETENTION_BYTES), "-2") && configs.get(LOCAL_RETENTION_BYTES) != null) {
      keys.add(LOCAL_RETENTION_BYTES);
      values.add(parseLong(configs.get(LOCAL_RETENTION_BYTES)));
    }
    keys.add(SEGMENT_BYTES);
    if (configs.get(SEGMENT_BYTES) != null) {
      values.add(parseLong(configs.get(SEGMENT_BYTES)));
    } else {
      values.add(1073741824L);
    }
    keys.add(MAX_MESSAGE_BYTES);
    if (configs.get(MAX_MESSAGE_BYTES) != null) {
      values.add(parseLong(configs.get(MAX_MESSAGE_BYTES)));
    } else {
      values.add(1048588L);
    }

    for (int i = 0; i < values.size() - 1; i++) {
      Long current = values.get(i);
      Long next = values.get(i + 1);
      if (current != 0 && next != 0 && current < next) {

        throw new IllegalArgumentException(
            String.format("Invalid configuration: %s (%s) should be greater than or equal to %s (%s)",
                keys.get(i), current,
                keys.get(i + 1), next
                ));
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
