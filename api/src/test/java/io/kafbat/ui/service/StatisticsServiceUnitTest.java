package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableTable;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.mapper.QuorumInfoMapper;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.ServerStatusDTO;
import io.kafbat.ui.model.Statistics;
import io.kafbat.ui.service.metrics.scrape.MetricsScraper;
import io.kafbat.ui.service.metrics.scrape.jmx.JmxMetricsRetriever;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class StatisticsServiceUnitTest {

  private static final String TOPIC_NAME = "list_offsets_timeout_topic";

  @Test
  void scrapeDegradesGracefullyWhenListOffsetsTimesOut() {
    // given - a cluster (e.g. Confluent Cloud) where listOffsets() times out, as reported in
    // https://github.com/kafbat/kafka-ui/issues/1852
    ReactiveAdminClient adminClient = mock(ReactiveAdminClient.class);
    AdminClientService adminClientService = mock(AdminClientService.class);
    KafkaConnectService kafkaConnectService = mock(KafkaConnectService.class);
    FeatureService featureService = mock(FeatureService.class);

    ClustersProperties clustersProperties = new ClustersProperties();
    KafkaCluster cluster = clusterWithInferredMetricsOnly(clustersProperties);

    when(adminClientService.get(cluster)).thenReturn(Mono.just(adminClient));
    when(kafkaConnectService.scrapeAllConnects(cluster)).thenReturn(Flux.empty());
    when(featureService.getAvailableFeatures(any(), any(), any())).thenReturn(Mono.just(List.of()));

    stubAdminClient(adminClient);

    StatisticsCache cache = mock(StatisticsCache.class);
    QuorumInfoMapper quorumInfoMapper = mock(QuorumInfoMapper.class);
    StatisticsService statisticsService = new StatisticsService(
        adminClientService, kafkaConnectService, featureService, cache, clustersProperties, quorumInfoMapper);

    // when - then: the whole scrape must still complete (status ONLINE, topic present) and
    // simply have no offset info, rather than failing entirely and losing topic/broker info too
    Statistics updated = statisticsService.updateCache(cluster).block();

    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(ServerStatusDTO.ONLINE);
    assertThat(updated.topicDescriptions()).extracting(TopicDescription::name).contains(TOPIC_NAME);
    var topicState = updated.getClusterState().getTopicStates().get(TOPIC_NAME);
    assertThat(topicState).isNotNull();
    assertThat(topicState.endOffsets()).isEmpty();
    assertThat(topicState.startOffsets()).isEmpty();
  }

  private static KafkaCluster clusterWithInferredMetricsOnly(ClustersProperties clustersProperties) {
    ClustersProperties.Cluster clusterProps = new ClustersProperties.Cluster();
    clusterProps.setName("test-cluster");
    clusterProps.setBootstrapServers("localhost:9092");
    // metrics == null -> inferred-only metrics scraper (no broker metrics)
    return KafkaCluster.builder()
        .name(clusterProps.getName())
        .metricsScrapping(MetricsScraper.create(clusterProps, mock(JmxMetricsRetriever.class)))
        .build();
  }

  private static void stubAdminClient(ReactiveAdminClient adminClient) {
    var node = new Node(0, "localhost", 9092);
    when(adminClient.describeCluster())
        .thenReturn(Mono.just(new ReactiveAdminClient.ClusterDescription(
            node, "test-cluster-id", List.of(node), Set.of())));
    when(adminClient.updateInternalStats(any())).thenReturn(Mono.empty());
    when(adminClient.describeLogDirs(anyList())).thenReturn(Mono.just(Map.of()));
    when(adminClient.listConsumerGroups()).thenReturn(Mono.just(List.of()));
    when(adminClient.describeTopics())
        .thenReturn(Mono.just(Map.of(TOPIC_NAME,
            new TopicDescription(TOPIC_NAME, false, List.of(new TopicPartitionInfo(0, null, List.of(), List.of()))))));
    when(adminClient.getTopicsConfig()).thenReturn(Mono.just(Map.of()));
    when(adminClient.listOffsets(any(), any(OffsetSpec.class)))
        .thenReturn(Mono.error(new TimeoutException("Timed out waiting for offsets")));
    when(adminClient.describeConsumerGroups(any())).thenReturn(Mono.just(Map.of()));
    when(adminClient.listConsumerGroupOffsets(any(), isNull())).thenReturn(Mono.just(ImmutableTable.of()));
    when(adminClient.describeMetadataQuorum()).thenReturn(Mono.error(new UnsupportedVersionException("test")));
    when(adminClient.getVersion()).thenReturn("3.7");
  }
}