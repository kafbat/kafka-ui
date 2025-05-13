package io.kafbat.ui.service.metrics.sink;

import static io.kafbat.ui.service.MessagesService.createProducer;
import static io.kafbat.ui.service.metrics.prometheus.PrometheusExpose.escapedLabelValue;
import static io.kafbat.ui.util.MetricsUtils.readPointValue;
import static io.kafbat.ui.util.MetricsUtils.toGoString;
import static org.apache.kafka.clients.producer.ProducerConfig.COMPRESSION_TYPE_CONFIG;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.kafbat.ui.config.ClustersProperties;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

/*
 * Format of records copied from https://github.com/Telefonica/prometheus-kafka-adapter
 */
@RequiredArgsConstructor
class KafkaSink implements MetricsSink {

  record KafkaMetric(String timestamp, String value, String name, Map<String, String> labels) { }

  private static final JsonMapper JSON_MAPPER = new JsonMapper();

  private static final Map<String, Object> PRODUCER_ADDITIONAL_CONFIGS = Map.of(COMPRESSION_TYPE_CONFIG, "gzip");

  private final String topic;
  private final Producer<byte[], byte[]> producer;

  static KafkaSink create(ClustersProperties.Cluster cluster, String targetTopic) {
    return new KafkaSink(targetTopic, createProducer(cluster, PRODUCER_ADDITIONAL_CONFIGS));
  }

  @Override
  public Mono<Void> send(Stream<MetricSnapshot> metrics) {
    return Mono.fromRunnable(() -> {
      String ts = Instant.now()
          .truncatedTo(ChronoUnit.SECONDS)
          .atZone(ZoneOffset.UTC)
          .format(DateTimeFormatter.ISO_DATE_TIME);

      metrics.flatMap(m -> createRecord(ts, m)).forEach(producer::send);
    });
  }

  private Stream<ProducerRecord<byte[], byte[]>> createRecord(String ts, MetricSnapshot metric) {
    String name = metric.getMetadata().getName();
    return metric.getDataPoints().stream()
        .map(sample -> {
          var lbls = new LinkedHashMap<String, String>();
          lbls.put("__name__", name);

          for (Label label : sample.getLabels()) {
            lbls.put(label.getName(), escapedLabelValue(label.getValue()));
          }

          var km = new KafkaMetric(ts, toGoString(readPointValue(sample)), name, lbls);
          return new ProducerRecord<>(topic, toJsonBytes(km));
        });
  }

  @SneakyThrows
  private static byte[] toJsonBytes(KafkaMetric m) {
    return JSON_MAPPER.writeValueAsBytes(m);
  }

}
