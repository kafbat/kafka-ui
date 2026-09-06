package io.kafbat.ui.service.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.producer.KafkaTestProducer;
import io.kafbat.ui.service.ClustersStorage;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;
import java.util.function.IntSupplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.awaitility.Awaitility;


class TopicAnalysisServiceTest extends AbstractIntegrationTest {

  @Autowired
  private ClustersStorage clustersStorage;

  @Autowired
  private TopicAnalysisService topicAnalysisService;

  @Test
  void savesResultWhenAnalysisIsCompleted() {
    String topic = "analyze_test_" + UUID.randomUUID();
    createTopic(new NewTopic(topic, 2, (short) 1));
    fillTopic(topic, 1_000);

    var cluster = clustersStorage.getClusterByName(LOCAL).orElseThrow();
    topicAnalysisService.analyze(cluster, topic).block();

    Awaitility.await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(() -> assertThat(topicAnalysisService.getTopicAnalysis(cluster, topic))
            .hasValueSatisfying(state -> {
              assertThat(state.getProgress()).isNull();
              assertThat(state.getResult()).isNotNull();
              var completedAnalyze = state.getResult();
              assertThat(completedAnalyze.getTotalStats().getTotalMsgs()).isEqualTo(1_000);
              assertThat(completedAnalyze.getPartitionStats().size()).isEqualTo(2);
            }));
  }

  @Test
  void topicStatsMatchPartitionStatsForSinglePartitionTopic() {
    String topic = "analyze_test_" + UUID.randomUUID();
    createTopic(new NewTopic(topic, 1, (short) 1));
    var rnd = new Random();
    fillTopic(topic, 1_000, () -> 1 + rnd.nextInt(2_048));

    var cluster = clustersStorage.getClusterByName(LOCAL).orElseThrow();
    topicAnalysisService.analyze(cluster, topic).block();

    Awaitility.await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(() -> assertThat(topicAnalysisService.getTopicAnalysis(cluster, topic))
            .hasValueSatisfying(state -> {
              assertThat(state.getResult()).isNotNull();
              var completedAnalyze = state.getResult();
              assertThat(completedAnalyze.getPartitionStats()).hasSize(1);
              // the only partition holds every message, so topic-wide stats must repeat it exactly -
              // percentiles included, see https://github.com/kafbat/kafka-ui/issues/374
              assertThat(completedAnalyze.getTotalStats())
                  .usingRecursiveComparison()
                  .ignoringFields("partition")
                  .isEqualTo(completedAnalyze.getPartitionStats().get(0));
            }));
  }

  private void fillTopic(String topic, int cnt) {
    fillTopic(topic, cnt, () -> 10);
  }

  private void fillTopic(String topic, int cnt, IntSupplier valueLength) {
    try (var producer = KafkaTestProducer.forKafka(kafka)) {
      for (int i = 0; i < cnt; i++) {
        producer.send(
            new ProducerRecord<>(
                topic,
                RandomStringUtils.secure().nextAlphabetic(5),
                RandomStringUtils.secure().nextAlphabetic(valueLength.getAsInt())));
      }
    }
  }

}
