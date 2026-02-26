package io.kafbat.ui.service.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class KafkaPropertiesConstraintsValidatorTest {

  @Nested
  class ValidateTest {
    @Test
    void shouldNotThrowForValidConfigs() {
      Map<String, String> config = new HashMap<>();
      config.put("min.insync.replicas", "2");
      config.put("compression.type", "gzip");
      config.put("compression.gzip.level", "7");
      config.put("cleanup.policy", "compact");
      config.put("min.cleanable.dirty.ratio", "0.7");
      config.put("min.compaction.lag.ms", "1000");
      config.put("max.compaction.lag.ms", "6000");
      config.put("delete.retention.ms", "30000");
      config.put("retention.ms", "704800000");
      config.put("local.retention.ms", "704800000");
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
      config.put("message.timestamp.after.max.ms", "8223372036854775807");
      config.put("message.timestamp.before.max.ms", "9222372036854775807");
      config.put("message.timestamp.difference.max.ms", "7223372036854775807");
      config.put("segment.index.bytes", "9485760");

      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      assertDoesNotThrow(validator::validate);
    }

    @Test
    void shouldNotThrowForDefaultConfigValues() {
      Map<String, String> config = new HashMap<>();
      config.put("min.insync.replicas", "1");
      config.put("compression.type", "producer");
      config.put("compression.gzip.level", "-1");
      config.put("compression.lz4.level", "9");
      config.put("compression.zstd.level", "3");
      config.put("cleanup.policy", "delete");
      config.put("min.cleanable.dirty.ratio", "0.5");
      config.put("min.compaction.lag.ms", "0");
      config.put("max.compaction.lag.ms", "9223372036854775807");
      config.put("delete.retention.ms", "86400000");
      config.put("retention.ms", "604800000");
      config.put("local.retention.ms", "-2");
      config.put("segment.ms", "604800000");
      config.put("retention.bytes", "-1");
      config.put("local.retention.bytes", "-2");
      config.put("segment.bytes", "1073741824");
      config.put("max.message.bytes", "1048588");
      config.put("leader.replication.throttled.replicas", "");
      config.put("remote.storage.enable", "false");
      config.put("message.downconversion.enable", "true");
      config.put("segment.jitter.ms", "0");
      config.put("flush.ms", "9223372036854775807");
      config.put("follower.replication.throttled.replicas", "");
      config.put("flush.messages", "9223372036854775807");
      config.put("message.format.version", "3.0-IV1");
      config.put("file.delete.delay.ms", "60000");
      config.put("message.timestamp.type", "CreateTime");
      config.put("preallocate", "false");
      config.put("index.interval.bytes", "4096");
      config.put("unclean.leader.election.enable", "false");
      config.put("message.timestamp.after.max.ms", "9223372036854775807");
      config.put("message.timestamp.before.max.ms", "9223372036854775807");
      config.put("message.timestamp.difference.max.ms", "9223372036854775807");
      config.put("segment.index.bytes", "10485760");

      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      assertDoesNotThrow(validator::validate);
    }

    @Test
    void shouldThrowForInValidConfigs() {
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
    void shouldNotThrowForPartialConfigs() {
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
    void shouldThrowForMinInSyncReplicasGreaterThanReplicationFactor() {
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
    void shouldThrowForInvalidCompressionConfigValues() {
      Map<String, String> config = Map.of("compression.type", "gzip",
          "compression.gzip.level", "5",
          "compression.zstd.level", "4");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("compression.zstd.level (4) should be set only when compression.type is zstd",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidCompactionConfigValues() {
      Map<String, String> config = Map.of("cleanup.policy", "delete",
          "min.cleanable.dirty.ratio", "0.7");
      int replicationFactor = 1;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("min.cleanable.dirty.ratio (0.7) should be set only when cleanup.policy is compact",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidRemoteStorageConfigValues() {
      Map<String, String> config = Map.of("local.retention.ms", "604800000",
          "local.retention.bytes", "1073741824");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);

      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::validate
      );
      assertEquals("local.retention.ms (604800000) should be set only when remoteStorageEnabled is true",
          exception.getMessage());
    }

    @Test
    void shouldThrowForInvalidRetentionAndDeletionTimeConfigs() {
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
    void shouldThrowForInvalidRetentionAndDeletionMemoryConfigs() {
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
    void shouldNotThrowForValidMinInSyncReplicas() {
      Map<String, String> config = Map.of("min.insync.replicas", "2");
      int replicationFactor = 3;

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(replicationFactor,
          config);
      assertDoesNotThrow(validator::minInSyncReplicasLessThanReplicationFactorValidation);
    }

    @Test
    void shouldThrowForMinInSyncReplicasGreaterThanReplicationFactor() {
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
    void shouldNotThrowForValidCompressionTypeAndConfig() {
      Map<String, String> configs = Map.of("compression.gzip.level", "5",
          "compression.type", "gzip");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compressionConfigValueValidation);
    }

    @Test
    void shouldThrowForNullCompressionType() {
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
    void shouldThrowForCompressionZstdLevelAndNonZstdCompressionType() {
      Map<String, String> configs = Map.of("compression.zstd.level", "4",
          "compression.type", "gzip");

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::compressionConfigValueValidation
      );
      assertEquals("compression.zstd.level (4) should be set only when compression.type is zstd",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForDefaultCompressionZstdLevelAndNonZstdCompressionType() {
      Map<String, String> configs = Map.of("compression.zstd.level", "3",
          "compression.type", "gzip");

      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);

      assertDoesNotThrow(validator::compressionConfigValueValidation);
    }


    @Test
    void shouldThrowForCompressionLz4LevelAndNonLz4CompressionType() {
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
    void shouldNotThrowForDefaultCompressionLz4LevelAndNonNullLz4CompressionType() {
      Map<String, String> configs = Map.of("compression.lz4.level", "9",
          "compression.type", "gzip");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compressionConfigValueValidation);
    }

    @Test
    void shouldThrowForCompressionGzipLevelAndNonGzipCompressionType() {
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

    @Test
    void shouldThrowForDefaultCompressionGzipLevelAndNonGzipCompressionType() {
      Map<String, String> configs = Map.of("compression.gzip.level", "-1",
          "compression.type", "zstd");
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compressionConfigValueValidation);
    }
  }

  @Nested
  class CompactionConfigValuesValidationTest {
    @Test
    void shouldNotThrowForNullConfigAndPolicies() {
      Map<String, String> configs = new HashMap<>();
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

    @Test
    void shouldNotThrowForNonNullConfigAndPolicies() {
      Map<String, String> config = Map.of(
          "min.cleanable.dirty.ratio", "0.7",
          "min.compaction.lag.ms", "1000",
          "max.compaction.lag.ms", "60000",
          "delete.retention.ms", "30000",
          "cleanup.policy", "compact"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, config);
      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

    @ParameterizedTest
    @MethodSource("nonDefaultCompactionArgs")
    void shouldThrowForDeletePolicyWhenCompactionOnlySettingsPresent(String key, String value, String expectedMsg) {
      Map<String, String> config = Map.of(
          key, value,
          "cleanup.policy", "delete"
      );

      var validator = new KafkaPropertiesConstraintsValidator(1, config);

      IllegalArgumentException ex = assertThrows(
          IllegalArgumentException.class,
          validator::compactionConfigValuesValidation
      );
      assertEquals(expectedMsg, ex.getMessage());
    }

    static Stream<Arguments> nonDefaultCompactionArgs() {
      return Stream.of(
          Arguments.of(
              "min.cleanable.dirty.ratio", "0.7",
              "min.cleanable.dirty.ratio (0.7) should be set only when cleanup.policy is compact"
          ),
          Arguments.of(
              "min.compaction.lag.ms", "1000",
              "min.compaction.lag.ms (1000) should be set only when cleanup.policy is compact"
          ),
          Arguments.of(
              "max.compaction.lag.ms", "6000",
              "max.compaction.lag.ms (6000) should be set only when cleanup.policy is compact"
          ),
          Arguments.of(
              "delete.retention.ms", "6000",
              "delete.retention.ms (6000) should be set only when cleanup.policy is compact"
          )
      );
    }

    @ParameterizedTest
    @MethodSource("defaultCompactionArgs")
    void shouldNotThrowForDeletePolicyWhenDefaultCompactionOnlySettingsPresent(String key, String value) {
      Map<String, String> config = Map.of(
          key, value,
          "cleanup.policy", "delete"
      );

      var validator = new KafkaPropertiesConstraintsValidator(1, config);

      assertDoesNotThrow(validator::compactionConfigValuesValidation);
    }

    static Stream<Arguments> defaultCompactionArgs() {
      return Stream.of(
          Arguments.of(
              "min.cleanable.dirty.ratio", "0.5"
          ),
          Arguments.of(
              "min.compaction.lag.ms", "0"
          ),
          Arguments.of(
              "max.compaction.lag.ms", "9223372036854775807"
          ),
          Arguments.of(
              "delete.retention.ms", "86400000"
          )
      );
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
    void shouldNotThrowForDefaultLocalRetentionValuesAndRemoteStorageEnabled() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.ms", "-2",
          "local.retention.bytes", "-2"
      );
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
          "retention.ms", "704800000",
          "local.retention.ms", "703800000",
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
    void shouldNotThrowForDefaultNullRetentionMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.ms", "304800000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldThrowForDefaultNullRetentionMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "local.retention.ms", "704800000",
          "segment.ms", "86400000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: retention.ms (604800000) "
          + "should be greater than or equal to local.retention.ms (704800000)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForDefaultNullLocalRetentionMs() {
      Map<String, String> configs = Map.of(
          "retention.ms", "1209600000",
          "segment.ms", "304800000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForDefaultNullSegmentMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "1209600000",
          "local.retention.ms", "604800000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldThrowForDefaultNullSegmentMs() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.ms", "1209600000",
          "local.retention.ms", "304800000"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionTimeConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: local.retention.ms (304800000) "
          + "should be greater than or equal to segment.ms (604800000)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForSentinelValues() {
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
          "max.message.bytes", "1087576"
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
    void shouldNotThrowForDefaultNullRetentionBytes() {
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
    void shouldNotThrowForDefaultNullLocalRetentionBytes() {
      Map<String, String> configs = Map.of(
          "retention.bytes", "1073741824",
          "segment.bytes", "10485760",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldNotThrowForDefaultNullSegmentBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1084741824",
          "local.retention.bytes", "1073741824",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      assertDoesNotThrow(validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation);
    }

    @Test
    void shouldThrowForDefaultNullSegmentBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1084741824",
          "local.retention.bytes", "1060741824",
          "max.message.bytes", "1048576"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: local.retention.bytes (1060741824) "
          + "should be greater than or equal to segment.bytes (1073741824)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForDefaultNullMaxMessageBytes() {
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
    void shouldThrowForDefaultNullMaxMessageBytes() {
      Map<String, String> configs = Map.of(
          "remote.storage.enable", "true",
          "retention.bytes", "1073741824",
          "local.retention.bytes", "1073741824",
          "segment.bytes", "1025760"
      );
      KafkaPropertiesConstraintsValidator validator = new KafkaPropertiesConstraintsValidator(1, configs);
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          validator::retentionAndDeletionMemoryConfigurationBasedConstraintsValidation
      );
      assertEquals("Invalid configuration: segment.bytes (1025760) "
          + "should be greater than or equal to max.message.bytes (1048588)",
          exception.getMessage());
    }

    @Test
    void shouldNotThrowForSentinelValues() {
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
