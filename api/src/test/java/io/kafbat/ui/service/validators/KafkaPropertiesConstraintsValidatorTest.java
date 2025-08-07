package io.kafbat.ui.service.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class KafkaPropertiesConstraintsValidatorTest {

  @Nested
  class ValidateTest {
    @Test
    public void shouldNotThrowForValidConfigs() {
      Map<String, String> config = new HashMap<>();
      config.put("min.insync.replicas", "2");
      config.put("compression.type", "gzip");
      config.put("compression.gzip.level", "3");
      config.put("cleanup.policy", "compact");
      config.put("min.cleanable.dirty.ratio", "0.5");
      config.put("min.compaction.lag.ms", "1000");
      config.put("max.compaction.lag.ms", "60000");
      config.put("delete.retention.ms", "30000");
      config.put("retention.ms", "604800000");
      config.put("local.retention.ms", "604800000");
      config.put("segment.ms", "86400000");
      config.put("retention.bytes", "1073741824");
      config.put("local.retention.bytes", "1073741824");
      config.put("segment.bytes", "10485760");
      config.put("max.message.bytes", "1048576");
      config.put("leader.replication.throttled.replicas", "replica1");
      config.put("remote.storage.enable", "true");
      config.put("message.downconversion.enable", "true");
      config.put("segment.jitter.ms", "1000");
      config.put("flush.ms", "5000");
      config.put("follower.replication.throttled.replicas", "replica2");
      config.put("flush.messages", "1000");
      config.put("message.format.version", "2.8-IV0");
      config.put("file.delete.delay.ms", "60000");
      config.put("message.timestamp.type", "CreateTime");
      config.put("preallocate", "false");
      config.put("index.interval.bytes", "4096");
      config.put("unclean.leader.election.enable", "true");
      config.put("message.timestamp.after.max.ms", "9223372036854775807");
      config.put("message.timestamp.before.max.ms", "9223372036854775807");
      config.put("message.timestamp.difference.max.ms", "9223372036854775807");
      config.put("segment.index.bytes", "10485760");

      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      assertDoesNotThrow(validator::validate);
    }

    @Test
    public void shouldThrowForInValidConfigs() {
      Map<String, String> config = new HashMap<>();
      config.put("min.insync.replicas", "2");
      config.put("compression.type", "gzip");
      config.put("compression.gzip.level", "3");
      config.put("cleanup.policy", "compact");
      config.put("min.cleanable.dirty.ratio", "0.5");
      config.put("min.compaction.lag.ms", "1000");
      config.put("max.compaction.lag.ms", "60000");
      config.put("delete.retention.ms", "30000");
      config.put("retention.ms", "604800000");
      config.put("local.retention.ms", "1296400000");
      config.put("segment.ms", "86400000");
      config.put("retention.bytes", "1073741824");
      config.put("local.retention.bytes", "1073741824");
      config.put("segment.bytes", "10485760");
      config.put("max.message.bytes", "1048576");
      config.put("leader.replication.throttled.replicas", "replica1");
      config.put("remote.storage.enable", "true");
      config.put("message.downconversion.enable", "true");
      config.put("segment.jitter.ms", "1000");
      config.put("flush.ms", "5000");
      config.put("follower.replication.throttled.replicas", "replica2");
      config.put("flush.messages", "1000");
      config.put("message.format.version", "2.8-IV0");
      config.put("file.delete.delay.ms", "60000");
      config.put("message.timestamp.type", "CreateTime");
      config.put("preallocate", "false");
      config.put("index.interval.bytes", "4096");
      config.put("unclean.leader.election.enable", "true");
      config.put("message.timestamp.after.max.ms", "9223372036854775807");
      config.put("message.timestamp.before.max.ms", "9223372036854775807");
      config.put("message.timestamp.difference.max.ms", "9223372036854775807");
      config.put("segment.index.bytes", "10485760");

      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("Invalid configuration: retention.ms (604800000) should be greater than or equal "
          + "to local.retention.ms (1296400000)",
          exception.getMessage());
    }

    @Test
    public void shouldNotThrowForPartialConfigs() {
      Map<String, String> config = new HashMap<>();
      config.put("min.insync.replicas", "2");
      config.put("compression.type", "gzip");
      config.put("compression.gzip.level", "3");
      config.put("cleanup.policy", "compact");
      config.put("min.cleanable.dirty.ratio", "0.5");
      config.put("min.compaction.lag.ms", "1000");
      config.put("max.compaction.lag.ms", "60000");
      config.put("delete.retention.ms", "30000");
      config.put("retention.ms", "604800000");
      config.put("segment.ms", "86400000");
      config.put("retention.bytes", "1073741824");
      config.put("local.retention.bytes", "1073741824");
      config.put("segment.bytes", "10485760");
      config.put("max.message.bytes", "1048576");
      config.put("leader.replication.throttled.replicas", "replica1");
      config.put("remote.storage.enable", "true");
      config.put("message.downconversion.enable", "true");
      config.put("segment.jitter.ms", "1000");
      config.put("flush.ms", "5000");
      config.put("follower.replication.throttled.replicas", "replica2");
      config.put("flush.messages", "1000");
      config.put("message.format.version", "2.8-IV0");
      config.put("file.delete.delay.ms", "60000");
      config.put("message.timestamp.type", "CreateTime");
      config.put("preallocate", "false");
      config.put("index.interval.bytes", "4096");
      config.put("unclean.leader.election.enable", "true");


      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      assertDoesNotThrow(validator::validate);
    }

    @Test
    public void shouldThrowForMinInSyncReplicasGreaterThanReplicationFactor() {
      Map<String, String> config = Map.of("min.insync.replicas", "4");
      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("min.insync.replicas (4) should be less than or equal to replication.factor (3)",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForInvalidCompressionConfigValues() {
      Map<String, String> config = Map.of("compression.type", "gzip",
                                          "compression.gzip.level", "5",
                                          "compression.zstd.level", "3");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("compression.zstd.level (3) should be set only when compression.type is zstd",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForInvalidCompactionConfigValues() {
      Map<String, String> config = Map.of("cleanup.policy", "delete",
                                          "min.cleanable.dirty.ratio", "0.5");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("min.cleanable.dirty.ratio (0.5) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForInvalidRemoteStorageConfigValues() {
      Map<String, String> config = Map.of("local.retention.ms", "604800000",
                                          "local.retention.bytes", "1073741824");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1,config);

      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("local.retention.ms (604800000) should be set only when remoteStorageEnabled is true",
          exception.getMessage());
    }
    @Test
    public void shouldThrowForInvalidRetentionAndDeletionTimeConfigs() {
      Map<String, String> config = Map.of("remote.storage.enable", "true",
                                          "retention.ms", "604800000",
                                          "local.retention.ms", "1209600000",
                                          "segment.ms", "86400000");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("Invalid configuration: retention.ms (604800000) "
          + "should be greater than or equal to local.retention.ms (1209600000)",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForInvalidRetentionAndDeletionMemoryConfigs() {
      Map<String, String> config = Map.of("remote.storage.enable", "true",
                                           "retention.bytes", "1073741824",
                                          "local.retention.bytes", "2147483648",
                                          "segment.bytes", "10485760",
                                          "max.message.bytes", "1048576");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("Invalid configuration: retention.bytes (1073741824) "
          + "should be greater than or equal to local.retention.bytes (2147483648)",
          exception.getMessage());
    }
  }

  @Nested
  class MinInSyncReplicasValidationTest {
    @Test
    public void shouldNotThrowForValidMinInSyncReplicas() {
      Map<String, String> config = Map.of("min.insync.replicas", "2");
      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      assertDoesNotThrow(validator::minInSyncReplicasLessThanReplicationFactorValidation);
    }

    @Test
    public void shouldThrowForMinInSyncReplicasGreaterThanReplicationFactor() {
      Map<String, String> config = Map.of("min.insync.replicas", "4");
      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
                                                                                              config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::minInSyncReplicasLessThanReplicationFactorValidation
      );
      assertEquals("min.insync.replicas (4) should be less than or equal to replication.factor (3)",
          exception.getMessage());
    }

  }

  @Nested
  class CompressionConfigValueValidationTest {
    @Test
    public void shouldNotThrowForValidCompressionTypeAndConfig() {
      Map<String, String> configs = Map.of("compression.gzip.level", "5",
                                          "compression.type", "gzip");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compressionConfigValueValidation);
    }

    @Test
    public void shouldThrowForNullCompressionType() {
      Map<String, String> configs = Map.of("compression.gzip.level", "5");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);

      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compressionConfigValueValidation
      );
      assertEquals("compression.gzip.level (5) should be set only when compression.type is gzip",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForCompressionZstdLevelAndNonZstdCompressionType() {
      Map<String, String> configs = Map.of("compression.zstd.level", "3",
                                          "compression.type", "gzip");

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compressionConfigValueValidation
      );
      assertEquals("compression.zstd.level (3) should be set only when compression.type is zstd",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForCompressionLz4LevelAndNonLz4CompressionType() {
      Map<String, String> configs = Map.of("compression.lz4.level", "3",
                                            "compression.type", "gzip");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compressionConfigValueValidation
      );
      assertEquals("compression.lz4.level (3) should be set only when compression.type is lz4",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForCompressionGzipLevelAndNonGzipCompressionType() {
      Map<String, String> configs = Map.of("compression.gzip.level", "5",
                                            "compression.type", "zstd");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compressionConfigValueValidation
      );
      assertEquals("compression.gzip.level (5) should be set only when compression.type is gzip",
          exception.getMessage());
    }
  }

  @Nested
  class CompactionConfigValuesValidationTest {
    @Test
    public void shouldNotThrowForNullConfigAndPolicies() {
      Map<String, String> configs = new HashMap<>();
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

    @Test
    public void shouldNotThrowForNonNullConfigAndPolicies() {
      Map<String, String> config = Map.of(
          "min.cleanable.dirty.ratio", "0.5",
          "min.compaction.lag.ms", "1000",
          "max.compaction.lag.ms", "60000",
          "delete.retention.ms", "30000",
          "cleanup.policy", "compact"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

    @Test
    public void shouldThrowDeleteCleanupPolicyAndNonNullMinCleanDirtyRatio() {
      Map<String, String> config = Map.of(
          "min.cleanable.dirty.ratio", "0.5",
          "cleanup.policy", "delete"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compactionConfigValuesValidation
      );
      assertEquals("min.cleanable.dirty.ratio (0.5) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForDeleteCleanupPolicyAndNonNullMinCompactionLag() {
      Map<String, String> config = Map.of(
          "min.compaction.lag.ms", "1000",
          "cleanup.policy", "delete"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compactionConfigValuesValidation
      );
      assertEquals("min.compaction.lag.ms (1000) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForDeleteCleanupPolicyAndNonNullMaxCompactionLag() {
      Map<String, String> config = Map.of(
          "max.compaction.lag.ms", "60000",
          "cleanup.policy", "delete"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compactionConfigValuesValidation
      );
      assertEquals("max.compaction.lag.ms (60000) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    public void shouldThrowForDeleteCleanupPolicyAndNonNullDeleteRetention() {
      Map<String, String> config = Map.of(
          "delete.retention.ms", "30000",
          "cleanup.policy", "delete"
      );

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compactionConfigValuesValidation
      );
      assertEquals("delete.retention.ms (30000) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    void shouldHandleCompactDeletePolicyCorrectly() {
      Map<String, String> config = Map.of(
          "delete.retention.ms", "30000",
          "cleanup.policy", "delete,compact"
      );

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

  }

  @Nested
  class RemoteStorageConfigValuesValidationTest {
    @Test
    void shouldNotThrowForNonNullLocalRetentionValuesAndRemoteStorageEnabled() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.ms", "604800000",
          "local.retention.bytes", "1073741824"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::remoteStorageConfigValuesValidation);
    }

    @Test
    void shouldNotThrowForNullLocalRetentionValuesAndRemoteStorageDisabled() {
      Map<String, String> configs = new HashMap<>();
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::remoteStorageConfigValuesValidation);
    }

    @Test
    void shouldThrowForNonNullLocalRetentionMsAndRemoteStorageDisabled() {
      Map<String, String> configs = Map.of(
          "local.retention.ms", "604800000",
          "local.retention.bytes", "1073741824"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::remoteStorageConfigValuesValidation
      );
      assertEquals("local.retention.ms (604800000) should be set only when remoteStorageEnabled is true",
          exception.getMessage());
    }

    @Test
    void shouldThrowForNonNullLocalRetentionBytesAndRemoteStorageDisabled() {
      Map<String, String> configs = Map.of(
          "local.retention.bytes", "1073741824"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::remoteStorageConfigValuesValidation
      );
      assertEquals("local.retention.bytes (1073741824) should be set only when remoteStorageEnabled is true",
          exception.getMessage());
    }
  }

  @Nested
  class RetentionAndDeletionTimeConfigurationBasedConstraintsValidationTest {

    @Test
    void shouldNotThrowForValidRetentionAndDeletionTimes() {
      Map<String, String> configs = Map.of(
              "remote.storage.enable", "true",
          "retention.ms", "604800000",
          "local.retention.ms", "604800000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldThrowForInvalidRetentionAndDeletionTimesForRetentionMsLessThanLocalRetentionMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "604800000",
          "local.retention.ms", "1209600000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: retention.ms (604800000) "
          + "should be greater than or equal to local.retention.ms (1209600000)",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidRetentionAndDeletionTimesForLocalRetentionMsLessThanSegmentMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "604800000",
          "local.retention.ms", "604800000",
          "segment.ms", "1209600000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: local.retention.ms (604800000) "
          + "should be greater than or equal to segment.ms (1209600000)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForNullSegmentMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "1209600000",
          "local.retention.ms", "604800000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForNullLocalRetentionMs() {
      Map<String, String> configs = Map.of(
          "retention.ms", "1209600000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForNullRetentionMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.ms", "604800000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForDefaultValues() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "-1",
          "local.retention.ms", "-2",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForSingleConfigValue() {
      Map<String, String> configs = Map.of(
          "retention.ms", "604800000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }


  }

  @Nested
  class RetentionAndDeletionMemoryConfigurationBasedConstraintsValidationTest {
    @Test
    void shouldNotThrowForValidMemoryConfigurations() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldThrowForInvalidMemoryConfigurationsForRetentionBytesLessThanLocalRetentionBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "2147483648",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: retention.bytes (1073741824) "
          + "should be greater than or equal to local.retention.bytes (2147483648)",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidMemoryConfigurationsForLocalRetentionBytesLessThanSegmentBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "2147483648",
          "max.message.bytes", "1048576"
      );

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: local.retention.bytes (1073741824) "
          + "should be greater than or equal to segment.bytes (2147483648)",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidMemoryConfigurationsForMaxMessageBytesLessThanSegmentBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "5242880",
          "max.message.bytes", "10485760"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: segment.bytes (5242880) "
          + "should be greater than or equal to max.message.bytes (10485760)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForNullRetentionBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForNullLocalRetentionBytes() {
      Map<String, String> configs = Map.of(
          "retention.bytes", "1073741824",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForNullSegmentBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForNullMaxMessageBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "10485760"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForDefaultValues() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "-1",
          "local.retention.bytes", "-2",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForSingleConfigValue() {
      Map<String, String> configs = Map.of(
          "retention.bytes", "1073741824"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForDoubleConfigValue() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }


  }
}
