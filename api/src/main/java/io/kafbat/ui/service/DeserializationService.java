package io.kafbat.ui.service;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.SerdeDescriptionDTO;
import io.kafbat.ui.serde.api.SchemaDescription;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.ClusterSerdes;
import io.kafbat.ui.serdes.ConsumerRecordDeserializer;
import io.kafbat.ui.serdes.ProducerRecordCreator;
import io.kafbat.ui.serdes.SerdeInstance;
import io.kafbat.ui.serdes.SerdesInitializer;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.validation.ValidationException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Class is responsible for managing serdes for kafka clusters.
 * NOTE: Since Serde interface is designed to be blocking it is required that DeserializationService
 * (and all Serde-related code) calls executed within special thread pool (boundedElastic).
 */
@Component
public class DeserializationService implements Closeable {

  private final Map<String, ClusterSerdes> clusterSerdes = new ConcurrentHashMap<>();

  public DeserializationService(Environment env,
                                ClustersStorage clustersStorage,
                                ClustersProperties clustersProperties) {
    var serdesInitializer = new SerdesInitializer();
    for (int i = 0; i < clustersProperties.getClusters().size(); i++) {
      var clusterProperties = clustersProperties.getClusters().get(i);
      var cluster = clustersStorage.getClusterByName(clusterProperties.getName()).get();
      clusterSerdes.put(cluster.getName(), serdesInitializer.init(env, clustersProperties, i));
    }
  }

  public ClusterSerdes getSerdesFor(String clusterName) {
    return clusterSerdes.get(clusterName);
  }

  private ClusterSerdes getSerdesFor(KafkaCluster cluster) {
    return getSerdesFor(cluster.getName());
  }

  private Serde.Serializer getSerializer(KafkaCluster cluster,
                                         String topic,
                                         Serde.Target type,
                                         String serdeName,
                                         @Nullable Map<String, Object> properties) {
    var serdes = getSerdesFor(cluster.getName());
    var serde = serdes.serdeForName(serdeName)
        .orElseThrow(() -> new ValidationException(
            String.format("Serde %s not found", serdeName)));
    if (!serde.canSerialize(topic, type)) {
      throw new ValidationException(
          String.format("Serde %s can't be applied for '%s' topic's %s serialization", serde, topic, type));
    }
    if (properties != null && !properties.isEmpty()) {
      return serde.serializer(topic, type, properties);
    }
    return serde.serializer(topic, type);
  }

  private SerdeInstance getSerdeForDeserialize(KafkaCluster cluster,
                                               String topic,
                                               Serde.Target type,
                                               @Nullable String serdeName) {
    var serdes = getSerdesFor(cluster);
    if (serdeName != null) {
      var serde = serdes.serdeForName(serdeName)
          .orElseThrow(() -> new ValidationException(String.format("Serde '%s' not found", serdeName)));
      if (!serde.canDeserialize(topic, type)) {
        throw new ValidationException(
            String.format("Serde '%s' can't be applied to '%s' topic %s", serdeName, topic, type));
      }
      return serde;
    } else {
      return serdes.suggestSerdeForDeserialize(topic, type);
    }
  }

  public ProducerRecordCreator producerRecordCreator(KafkaCluster cluster,
                                                     String topic,
                                                     String keySerdeName,
                                                     String valueSerdeName,
                                                     @Nullable Map<String, Object> keyProperties,
                                                     @Nullable Map<String, Object> valueProperties) {
    return new ProducerRecordCreator(
        getSerializer(cluster, topic, Serde.Target.KEY, keySerdeName, keyProperties),
        getSerializer(cluster, topic, Serde.Target.VALUE, valueSerdeName, valueProperties)
    );
  }

  public ConsumerRecordDeserializer deserializerFor(KafkaCluster cluster,
                                                    String topic,
                                                    @Nullable String keySerdeName,
                                                    @Nullable String valueSerdeName) {
    var keySerde = getSerdeForDeserialize(cluster, topic, Serde.Target.KEY, keySerdeName);
    var valueSerde = getSerdeForDeserialize(cluster, topic, Serde.Target.VALUE, valueSerdeName);
    var fallbackSerde = getSerdesFor(cluster).getFallbackSerde();
    return new ConsumerRecordDeserializer(
        keySerde.getName(),
        keySerde.deserializer(topic, Serde.Target.KEY),
        valueSerde.getName(),
        valueSerde.deserializer(topic, Serde.Target.VALUE),
        fallbackSerde.getName(),
        fallbackSerde.deserializer(topic, Serde.Target.KEY),
        fallbackSerde.deserializer(topic, Serde.Target.VALUE),
        cluster.getMasking().getMaskerForTopic(topic)
    );
  }

  public List<SerdeDescriptionDTO> getSerdesForSerialize(KafkaCluster cluster,
                                                         String topic,
                                                         Serde.Target serdeType) {
    var serdes = getSerdesFor(cluster);
    var preferred = serdes.suggestSerdeForSerialize(topic, serdeType);
    var result = new ArrayList<SerdeDescriptionDTO>();
    result.add(toDto(preferred, topic, serdeType, true));
    serdes.all()
        .filter(s -> !s.getName().equals(preferred.getName()))
        .filter(s -> s.canSerialize(topic, serdeType))
        .forEach(s -> result.add(toDto(s, topic, serdeType, false)));
    return result;
  }

  public List<SerdeDescriptionDTO> getSerdesForDeserialize(KafkaCluster cluster,
                                                           String topic,
                                                           Serde.Target serdeType) {
    var serdes = getSerdesFor(cluster);
    var preferred = serdes.suggestSerdeForDeserialize(topic, serdeType);
    var result = new ArrayList<SerdeDescriptionDTO>();
    result.add(toDto(preferred, topic, serdeType, true));
    serdes.all()
        .filter(s -> !s.getName().equals(preferred.getName()))
        .filter(s -> s.canDeserialize(topic, serdeType))
        .forEach(s -> result.add(toDto(s, topic, serdeType, false)));
    return result;
  }

  private SerdeDescriptionDTO toDto(SerdeInstance serdeInstance,
                                    String topic,
                                    Serde.Target serdeType,
                                    boolean preferred) {
    var schemaOpt = serdeInstance.getSchema(topic, serdeType);
    Map<String, Object> additionalProps = schemaOpt
        .map(SchemaDescription::getAdditionalProperties)
        .map(HashMap::new)
        .orElseGet(HashMap::new);

    var subjects = serdeInstance.getSubjects(topic, serdeType);
    if (!subjects.isEmpty()) {
      additionalProps.put("subjects", subjects);
    }

    return new SerdeDescriptionDTO()
        .name(serdeInstance.getName())
        .description(serdeInstance.description().orElse(null))
        .schema(schemaOpt.map(SchemaDescription::getSchema).orElse(null))
        .additionalProperties(additionalProps.isEmpty() ? null : additionalProps)
        .preferred(preferred);
  }

  @Override
  public void close() {
    clusterSerdes.values().forEach(ClusterSerdes::close);
  }
}
