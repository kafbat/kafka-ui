package io.kafbat.ui.serdes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.serde.api.PropertyResolver;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.builtin.AvroEmbeddedSerde;
import io.kafbat.ui.serdes.builtin.Base64Serde;
import io.kafbat.ui.serdes.builtin.ConsumerOffsetsSerde;
import io.kafbat.ui.serdes.builtin.HexSerde;
import io.kafbat.ui.serdes.builtin.Int32Serde;
import io.kafbat.ui.serdes.builtin.Int64Serde;
import io.kafbat.ui.serdes.builtin.ProtobufFileSerde;
import io.kafbat.ui.serdes.builtin.ProtobufRawSerde;
import io.kafbat.ui.serdes.builtin.StringSerde;
import io.kafbat.ui.serdes.builtin.UInt32Serde;
import io.kafbat.ui.serdes.builtin.UInt64Serde;
import io.kafbat.ui.serdes.builtin.UuidBinarySerde;
import io.kafbat.ui.serdes.builtin.mm2.CheckpointSerde;
import io.kafbat.ui.serdes.builtin.mm2.HeartbeatSerde;
import io.kafbat.ui.serdes.builtin.mm2.OffsetSyncSerde;
import io.kafbat.ui.serdes.builtin.sr.SchemaRegistrySerde;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import jakarta.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

@Slf4j
public class SerdesInitializer {

  private final Map<String, Class<? extends BuiltInSerde>> builtInSerdeClasses;
  private final CustomSerdeLoader customSerdeLoader;

  public SerdesInitializer() {
    this(
        ImmutableMap.<String, Class<? extends BuiltInSerde>>builder()
            .put(StringSerde.NAME, StringSerde.class)
            .put(SchemaRegistrySerde.NAME, SchemaRegistrySerde.class)
            .put(ProtobufFileSerde.NAME, ProtobufFileSerde.class)
            .put(Int32Serde.NAME, Int32Serde.class)
            .put(Int64Serde.NAME, Int64Serde.class)
            .put(UInt32Serde.NAME, UInt32Serde.class)
            .put(UInt64Serde.NAME, UInt64Serde.class)
            .put(AvroEmbeddedSerde.NAME, AvroEmbeddedSerde.class)
            .put(Base64Serde.NAME, Base64Serde.class)
            .put(HexSerde.NAME, HexSerde.class)
            .put(UuidBinarySerde.NAME, UuidBinarySerde.class)
            .put(ProtobufRawSerde.NAME, ProtobufRawSerde.class)

            // mm2 serdes
            .put(HeartbeatSerde.NAME, HeartbeatSerde.class)
            .put(OffsetSyncSerde.NAME, OffsetSyncSerde.class)
            .put(CheckpointSerde.NAME, CheckpointSerde.class)
            .build(),
        new CustomSerdeLoader()
    );
  }

  @VisibleForTesting
  SerdesInitializer(Map<String, Class<? extends BuiltInSerde>> builtInSerdeClasses,
                    CustomSerdeLoader customSerdeLoader) {
    this.builtInSerdeClasses = builtInSerdeClasses;
    this.customSerdeLoader = customSerdeLoader;
  }

  /**
   * Initialization algorithm:
   * First, we iterate over explicitly configured serdes from cluster config:
   * > if serde has name = one of built-in serde's names:
   * - if serde's properties are empty, we treat it as serde should be
   * auto-configured - we try to do that
   * - if serde's properties not empty, we treat it as an intention to
   * override default configuration, so we configuring it with specific config (calling configure(..))
   * <p/>
   * > if serde has className = one of built-in serde's classes:
   * - initializing it with specific config and with default classloader
   * <p/>
   * > if serde has custom className != one of built-in serde's classes:
   * - initializing it with specific config and with custom classloader (see CustomSerdeLoader)
   * <p/>
   * Second, we iterate over remaining built-in serdes (that we NOT explicitly configured by config)
   * trying to auto-configure them and  registering with empty patterns - they will be present
   * in Serde selection in UI, but not assigned to any topic k/v.
   */
  public ClusterSerdes init(Environment env,
                            ClustersProperties clustersProperties,
                            int clusterIndex) {
    ClustersProperties.Cluster clusterProperties = clustersProperties.getClusters().get(clusterIndex);
    log.debug("Configuring serdes for cluster {}", clusterProperties.getName());

    var globalPropertiesResolver = new PropertyResolverImpl(env);
    var clusterPropertiesResolver = new PropertyResolverImpl(env, "kafka.clusters." + clusterIndex);

    Map<String, SerdeInstance> registeredSerdes = new LinkedHashMap<>();
    // initializing serdes from config
    if (clusterProperties.getSerde() != null) {
      for (int i = 0; i < clusterProperties.getSerde().size(); i++) {
        ClustersProperties.SerdeConfig serdeConfig = clusterProperties.getSerde().get(i);
        if (Strings.isNullOrEmpty(serdeConfig.getName())) {
          throw new ValidationException("'name' property not set for serde: " + serdeConfig);
        }
        if (registeredSerdes.containsKey(serdeConfig.getName())) {
          throw new ValidationException("Multiple serdes with same name: " + serdeConfig.getName());
        }
        var instance = createSerdeFromConfig(
            serdeConfig,
            new PropertyResolverImpl(env, "kafka.clusters." + clusterIndex + ".serde." + i + ".properties"),
            clusterPropertiesResolver,
            globalPropertiesResolver
        );
        registeredSerdes.put(serdeConfig.getName(), instance);
      }
    }

    // initializing remaining built-in serdes with empty selection patters
    builtInSerdeClasses.forEach((name, clazz) -> {
      if (!registeredSerdes.containsKey(name)) {
        BuiltInSerde serde = createSerdeInstance(clazz);
        if (autoConfigureSerde(serde, clusterPropertiesResolver, globalPropertiesResolver)) {
          registeredSerdes.put(name, new SerdeInstance(name, serde, null, null, null));
        }
      }
    });

    registerTopicRelatedSerde(registeredSerdes);

    return new ClusterSerdes(
        registeredSerdes,
        Optional.ofNullable(clusterProperties.getDefaultKeySerde())
            .map(name -> Preconditions.checkNotNull(registeredSerdes.get(name), "Default key serde not found"))
            .orElse(null),
        Optional.ofNullable(clusterProperties.getDefaultValueSerde())
            .map(name -> Preconditions.checkNotNull(registeredSerdes.get(name), "Default value serde not found"))
            .or(() -> Optional.ofNullable(registeredSerdes.get(SchemaRegistrySerde.NAME)))
            .or(() -> Optional.ofNullable(registeredSerdes.get(ProtobufFileSerde.NAME)))
            .orElse(null),
        createFallbackSerde()
    );
  }

  /**
   * Registers serdse that should only be used for specific (hard-coded) topics, like ConsumerOffsetsSerde.
   */
  private void registerTopicRelatedSerde(Map<String, SerdeInstance> serdes) {
    serdes.putAll(consumerOffsetsSerde());
    serdes.putAll(mirrorMakerSerdes());
  }

  private Map<String, SerdeInstance> consumerOffsetsSerde() {
    var pattern = Pattern.compile(ConsumerOffsetsSerde.TOPIC);
    return Map.of(
        ConsumerOffsetsSerde.NAME,
        new SerdeInstance(
            ConsumerOffsetsSerde.NAME,
            new ConsumerOffsetsSerde(),
            pattern,
            pattern,
            null
        )
    );
  }

  private Map<String, SerdeInstance> mirrorMakerSerdes() {
    return Map.of(
        HeartbeatSerde.NAME,
        mirrorSerde(HeartbeatSerde.NAME, HeartbeatSerde.TOPIC_NAME_PATTERN, new HeartbeatSerde()),
        OffsetSyncSerde.NAME,
        mirrorSerde(OffsetSyncSerde.NAME, OffsetSyncSerde.TOPIC_NAME_PATTERN, new OffsetSyncSerde()),
        CheckpointSerde.NAME,
        mirrorSerde(CheckpointSerde.NAME, CheckpointSerde.TOPIC_NAME_PATTERN, new CheckpointSerde())
    );
  }

  private SerdeInstance mirrorSerde(String name, Pattern pattern, BuiltInSerde serde) {
    return new SerdeInstance(name, serde, pattern, pattern, null);
  }

  private SerdeInstance createFallbackSerde() {
    StringSerde serde = new StringSerde();
    serde.configure(PropertyResolverImpl.empty(), PropertyResolverImpl.empty(), PropertyResolverImpl.empty());
    return new SerdeInstance("Fallback", serde, null, null, null);
  }

  @SneakyThrows
  private SerdeInstance createSerdeFromConfig(ClustersProperties.SerdeConfig serdeConfig,
                                              PropertyResolver serdeProps,
                                              PropertyResolver clusterProps,
                                              PropertyResolver globalProps) {
    if (builtInSerdeClasses.containsKey(serdeConfig.getName())) {
      return createSerdeWithBuiltInSerdeName(serdeConfig, serdeProps, clusterProps, globalProps);
    }
    if (serdeConfig.getClassName() != null) {
      var builtInSerdeClass = builtInSerdeClasses.values().stream()
          .filter(c -> c.getName().equals(serdeConfig.getClassName()))
          .findAny();
      // built-in serde type with custom name
      if (builtInSerdeClass.isPresent()) {
        return createSerdeWithBuiltInClass(builtInSerdeClass.get(), serdeConfig, serdeProps, clusterProps, globalProps);
      }
    }
    log.info("Loading custom serde {}", serdeConfig.getName());
    return loadAndInitCustomSerde(serdeConfig, serdeProps, clusterProps, globalProps);
  }

  private SerdeInstance createSerdeWithBuiltInSerdeName(ClustersProperties.SerdeConfig serdeConfig,
                                                        PropertyResolver serdeProps,
                                                        PropertyResolver clusterProps,
                                                        PropertyResolver globalProps) {
    String name = serdeConfig.getName();
    if (serdeConfig.getClassName() != null) {
      throw new ValidationException("className can't be set for built-in serde");
    }
    if (serdeConfig.getFilePath() != null) {
      throw new ValidationException("filePath can't be set for built-in serde types");
    }
    var clazz = builtInSerdeClasses.get(name);
    BuiltInSerde serde = createSerdeInstance(clazz);
    if (serdeConfig.getProperties() == null || serdeConfig.getProperties().isEmpty()) {
      if (!autoConfigureSerde(serde, clusterProps, globalProps)) {
        // no properties provided and serde does not support auto-configuration
        throw new ValidationException(name + " serde is not configured");
      }
    } else {
      // configuring serde with explicitly set properties
      serde.configure(serdeProps, clusterProps, globalProps);
    }
    return new SerdeInstance(
        name,
        serde,
        nullablePattern(serdeConfig.getTopicKeysPattern()),
        nullablePattern(serdeConfig.getTopicValuesPattern()),
        null
    );
  }

  private boolean autoConfigureSerde(BuiltInSerde serde, PropertyResolver clusterProps, PropertyResolver globalProps) {
    if (serde.canBeAutoConfigured(clusterProps, globalProps)) {
      serde.autoConfigure(clusterProps, globalProps);
      return true;
    }
    return false;
  }

  @SneakyThrows
  private SerdeInstance createSerdeWithBuiltInClass(Class<? extends BuiltInSerde> clazz,
                                                    ClustersProperties.SerdeConfig serdeConfig,
                                                    PropertyResolver serdeProps,
                                                    PropertyResolver clusterProps,
                                                    PropertyResolver globalProps) {
    if (serdeConfig.getFilePath() != null) {
      throw new ValidationException("filePath can't be set for built-in serde type");
    }
    BuiltInSerde serde = createSerdeInstance(clazz);
    serde.configure(serdeProps, clusterProps, globalProps);
    return new SerdeInstance(
        serdeConfig.getName(),
        serde,
        nullablePattern(serdeConfig.getTopicKeysPattern()),
        nullablePattern(serdeConfig.getTopicValuesPattern()),
        null
    );
  }

  @SneakyThrows
  private <T extends Serde> T createSerdeInstance(Class<T> clazz) {
    return clazz.getDeclaredConstructor().newInstance();
  }

  private SerdeInstance loadAndInitCustomSerde(ClustersProperties.SerdeConfig serdeConfig,
                                               PropertyResolver serdeProps,
                                               PropertyResolver clusterProps,
                                               PropertyResolver globalProps) {
    if (Strings.isNullOrEmpty(serdeConfig.getClassName())) {
      throw new ValidationException(
          "'className' property not set for custom serde " + serdeConfig.getName());
    }
    if (Strings.isNullOrEmpty(serdeConfig.getFilePath())) {
      throw new ValidationException(
          "'filePath' property not set for custom serde " + serdeConfig.getName());
    }
    var loaded = customSerdeLoader.loadAndConfigure(
        serdeConfig.getClassName(), serdeConfig.getFilePath(), serdeProps, clusterProps, globalProps);
    return new SerdeInstance(
        serdeConfig.getName(),
        loaded.getSerde(),
        nullablePattern(serdeConfig.getTopicKeysPattern()),
        nullablePattern(serdeConfig.getTopicValuesPattern()),
        loaded.getClassLoader()
    );
  }

  @Nullable
  private Pattern nullablePattern(@Nullable String pattern) {
    return pattern == null ? null : Pattern.compile(pattern);
  }
}
