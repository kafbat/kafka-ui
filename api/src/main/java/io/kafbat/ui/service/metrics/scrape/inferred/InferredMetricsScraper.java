package io.kafbat.ui.service.metrics.scrape.inferred;

import com.google.common.annotations.VisibleForTesting;
import io.kafbat.ui.model.ConnectorStateDTO;
import io.kafbat.ui.model.ConnectorTaskStatusDTO;
import io.kafbat.ui.service.metrics.scrape.KafkaConnectState;
import io.kafbat.ui.service.metrics.scrape.ScrapedClusterState;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Metric;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.common.Node;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class InferredMetricsScraper {

  public static final String NODE_ID_TAG = "node_id";
  public static final String TOPIC_TAG = "topic";
  public static final String GROUP_TAG = "group";
  public static final String PARTITION_TAG = "partition";
  public static final String CONNECT = "connect";
  public static final String CONNECTOR = "connector";
  private ScrapedClusterState prevState = null;

  public synchronized Mono<InferredMetrics> scrape(ScrapedClusterState newState, List<KafkaConnectState> connects) {
    var inferred = infer(prevState, newState, connects);
    this.prevState = newState;
    return Mono.just(inferred);
  }

  @VisibleForTesting
  static InferredMetrics infer(@Nullable ScrapedClusterState prevState, ScrapedClusterState newState,
                               List<KafkaConnectState> connects) {
    var registry = new MetricsRegistry();
    fillNodesMetrics(registry, newState);
    fillTopicMetrics(registry, newState);
    fillConsumerGroupsMetrics(registry, newState);
    fillKafkaConnectMetrics(registry, connects);
    List<MetricSnapshot> metrics = registry.metrics.values().stream().map(Metric::collect).toList();
    log.debug("{} metric families inferred from cluster state", metrics.size());
    return new InferredMetrics(metrics);
  }

  private static class MetricsRegistry {

    final Map<String, Metric> metrics = new LinkedHashMap<>();

    void gauge(String name,
               String help,
               List<String> lbls,
               List<String> lblVals,
               Number value) {


      Gauge gauge = (Gauge) metrics.computeIfAbsent(name, (n) ->
          Gauge.builder()
              .name(PrometheusNaming.sanitizeMetricName(name))
              .labelNames(lbls.toArray(new String[0]))
              .help(help).build()
      );

      gauge.labelValues(lblVals.toArray(new String[0]))
          .set(value.doubleValue());
    }
  }

  private static void fillNodesMetrics(MetricsRegistry registry, ScrapedClusterState newState) {
    registry.gauge(
        "broker_count",
        "Number of brokers in the Kafka cluster",
        List.of(),
        List.of(),
        newState.getNodesStates().size()
    );

    newState.getNodesStates().forEach((nodeId, state) -> {
      if (state.segmentStats() != null) {
        registry.gauge(
            "broker_bytes_disk",
            "Written disk size in bytes of a broker",
            List.of(NODE_ID_TAG),
            List.of(nodeId.toString()),
            state.segmentStats().getSegmentSize()
        );
      }
      if (state.logDirSpaceStats() != null) {
        if (state.logDirSpaceStats().usableBytes() != null) {
          registry.gauge(
              "broker_bytes_usable",
              "Usable disk size in bytes of a broker",
              List.of(NODE_ID_TAG),
              List.of(nodeId.toString()),
              state.logDirSpaceStats().usableBytes()
          );
        }
        if (state.logDirSpaceStats().totalBytes() != null) {
          registry.gauge(
              "broker_bytes_total",
              "Total disk size in bytes of a broker",
              List.of(NODE_ID_TAG),
              List.of(nodeId.toString()),
              state.logDirSpaceStats().totalBytes()
          );
        }
      }
    });
  }

  private static void fillTopicMetrics(MetricsRegistry registry, ScrapedClusterState clusterState) {
    registry.gauge(
        "topic_count",
        "Number of topics in the Kafka cluster",
        List.of(),
        List.of(),
        clusterState.getTopicStates().size()
    );

    clusterState.getTopicStates().forEach((topicName, state) -> {
      registry.gauge(
          "kafka_topic_partitions",
          "Number of partitions for this Topic",
          List.of(TOPIC_TAG),
          List.of(topicName),
          state.description().partitions().size()
      );
      state.endOffsets().forEach((partition, endOffset) -> registry.gauge(
          "kafka_topic_partition_next_offset",
          "Current (next) Offset of a Broker at Topic/Partition",
          List.of(TOPIC_TAG, PARTITION_TAG),
          List.of(topicName, String.valueOf(partition)),
          endOffset
      ));
      state.startOffsets().forEach((partition, startOffset) -> registry.gauge(
          "kafka_topic_partition_oldest_offset",
          "Oldest Offset of a Broker at Topic/Partition",
          List.of(TOPIC_TAG, PARTITION_TAG),
          List.of(topicName, String.valueOf(partition)),
          startOffset
      ));
      state.description().partitions().forEach(p -> {
        registry.gauge(
            "kafka_topic_partition_in_sync_replica",
            "Number of In-Sync Replicas for this Topic/Partition",
            List.of(TOPIC_TAG, PARTITION_TAG),
            List.of(topicName, String.valueOf(p.partition())),
            p.isr().size()
        );
        registry.gauge(
            "kafka_topic_partition_replicas",
            "Number of Replicas for this Topic/Partition",
            List.of(TOPIC_TAG, PARTITION_TAG),
            List.of(topicName, String.valueOf(p.partition())),
            p.replicas().size()
        );
        registry.gauge(
            "kafka_topic_partition_leader",
            "Leader Broker ID of this Topic/Partition (-1, if no leader)",
            List.of(TOPIC_TAG, PARTITION_TAG),
            List.of(topicName, String.valueOf(p.partition())),
            Optional.ofNullable(p.leader()).map(Node::id).orElse(-1)
        );
      });
      if (state.segmentStats() != null) {
        registry.gauge(
            "topic_bytes_disk",
            "Disk size in bytes of a topic",
            List.of(TOPIC_TAG),
            List.of(topicName),
            state.segmentStats().getSegmentSize()
        );
      }
    });
  }

  private static void fillConsumerGroupsMetrics(MetricsRegistry registry, ScrapedClusterState clusterState) {
    registry.gauge(
        "group_count",
        "Number of consumer groups in the Kafka cluster",
        List.of(),
        List.of(),
        clusterState.getConsumerGroupsStates().size()
    );

    clusterState.getConsumerGroupsStates().forEach((groupName, state) -> {
      registry.gauge(
          "group_state",
          "State of the consumer group, value = ordinal of org.apache.kafka.common.ConsumerGroupState",
          List.of(GROUP_TAG),
          List.of(groupName),
          state.description().state().ordinal()
      );
      registry.gauge(
          "group_member_count",
          "Number of member assignments in the consumer group.",
          List.of(GROUP_TAG),
          List.of(groupName),
          state.description().members().size()
      );
      registry.gauge(
          "group_host_count",
          "Number of distinct hosts in the consumer group.",
          List.of(GROUP_TAG),
          List.of(groupName),
          state.description().members().stream().map(MemberDescription::host).distinct().count()
      );

      state.committedOffsets().forEach((tp, committedOffset) -> {
        registry.gauge(
            "kafka_consumergroup_current_offset",
            "Current Offset of a ConsumerGroup at Topic/Partition",
            List.of("consumergroup", TOPIC_TAG, PARTITION_TAG),
            List.of(groupName, tp.topic(), String.valueOf(tp.partition())),
            committedOffset
        );

        Optional.ofNullable(clusterState.getTopicStates().get(tp.topic()))
            .flatMap(s -> Optional.ofNullable(s.endOffsets().get(tp.partition())))
            .ifPresent(endOffset ->
                registry.gauge(
                    "kafka_consumergroup_lag",
                    "Current Approximate Lag of a ConsumerGroup at Topic/Partition",
                    List.of("consumergroup", TOPIC_TAG, PARTITION_TAG),
                    List.of(groupName, tp.topic(), String.valueOf(tp.partition())),
                    endOffset - committedOffset
                ));

      });
    });
  }

  private static void fillKafkaConnectMetrics(MetricsRegistry registry, List<KafkaConnectState> connects) {
    registry.gauge("connect_count", "Number of Kafka Connect clusters configured for the Kafka cluster",
        List.of(), List.of(), connects.size());
    connects.forEach(connect -> {
      registry.gauge("connector_count", "Number of connectors in the Kafka Connect cluster",
          List.of(CONNECT), List.of(connect.getName()), connect.getConnectors().size());
      connect.getConnectors().forEach(c -> {
        registry.gauge("connector_task_count", "Number of tasks of the connector",
            List.of(CONNECT, CONNECTOR), List.of(connect.getName(), c.name()), c.tasks().size());
        if (c.status() != null && c.status().getState() != null) {
          String type = c.connectorType() != null ? c.connectorType().name() : "UNKNOWN";
          fillStateSet(registry, "connector_state",
              "State of the connector, value = 1 for the current state, 0 otherwise",
              List.of(CONNECT, CONNECTOR, "connector_type", "state"),
              List.of(connect.getName(), c.name(), type), ConnectorStateDTO.values(), c.status().getState());
        }
        c.tasks().stream().filter(t -> t.getState() != null).forEach(t ->
            fillStateSet(registry, "connector_task_state",
                "State of the connector task, value = 1 for the current state, 0 otherwise",
                List.of(CONNECT, CONNECTOR, "task", "worker_id", "state"),
                List.of(connect.getName(), c.name(), String.valueOf(t.getId()),
                    Objects.requireNonNullElse(t.getWorkerId(), "unknown")),
                ConnectorTaskStatusDTO.values(), t.getState()));
      });
    });
  }

  private static <E extends Enum<E>> void fillStateSet(MetricsRegistry registry, String name, String help,
                                                       List<String> lbls, List<String> lblVals,
                                                       E[] states, E current) {
    for (E state : states) {
      registry.gauge(name, help, lbls, Stream.concat(lblVals.stream(), Stream.of(state.name())).toList(),
          state == current ? 1 : 0);
    }
  }
}
