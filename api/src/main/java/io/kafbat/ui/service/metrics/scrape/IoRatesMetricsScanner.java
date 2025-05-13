package io.kafbat.ui.service.metrics.scrape;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Scans external jmx/prometheus metric and tries to infer io rates
class IoRatesMetricsScanner {

  // per broker
  final Map<Integer, BigDecimal> brokerBytesInFifteenMinuteRate = new HashMap<>();
  final Map<Integer, BigDecimal> brokerBytesOutFifteenMinuteRate = new HashMap<>();

  // per topic
  final Map<String, BigDecimal> bytesInFifteenMinuteRate = new HashMap<>();
  final Map<String, BigDecimal> bytesOutFifteenMinuteRate = new HashMap<>();

  IoRatesMetricsScanner(Map<Integer, List<MetricSnapshot>> perBrokerMetrics) {
    for (Map.Entry<Integer, List<MetricSnapshot>> broker : perBrokerMetrics.entrySet()) {
      Integer nodeId = broker.getKey();
      List<MetricSnapshot> metrics = broker.getValue();
      for (MetricSnapshot metric : metrics) {
        String name = metric.getMetadata().getName();
        if (metric instanceof GaugeSnapshot gauge) {
          for (GaugeSnapshot.GaugeDataPointSnapshot dataPoint : gauge.getDataPoints()) {
            updateBrokerIOrates(nodeId, name, dataPoint);
            updateTopicsIOrates(name, dataPoint);
          }
        }
      }
    }
  }

  Metrics.IoRates get() {
    return Metrics.IoRates.builder()
        .topicBytesInPerSec(bytesInFifteenMinuteRate)
        .topicBytesOutPerSec(bytesOutFifteenMinuteRate)
        .brokerBytesInPerSec(brokerBytesInFifteenMinuteRate)
        .brokerBytesOutPerSec(brokerBytesOutFifteenMinuteRate)
        .build();
  }

  private void updateBrokerIOrates(int nodeId, String name, GaugeSnapshot.GaugeDataPointSnapshot point) {
    Labels labels = point.getLabels();
    if (!brokerBytesInFifteenMinuteRate.containsKey(nodeId)
        && labels.size() == 1
        && "BytesInPerSec".equalsIgnoreCase(labels.getValue(0))
        && containsIgnoreCase(name, "BrokerTopicMetrics")
        && endsWithIgnoreCase(name, "FifteenMinuteRate")) {
      brokerBytesInFifteenMinuteRate.put(nodeId, BigDecimal.valueOf(point.getValue()));
    }
    if (!brokerBytesOutFifteenMinuteRate.containsKey(nodeId)
        && labels.size() == 1
        && "BytesOutPerSec".equalsIgnoreCase(labels.getValue(0))
        && containsIgnoreCase(name, "BrokerTopicMetrics")
        && endsWithIgnoreCase(name, "FifteenMinuteRate")) {
      brokerBytesOutFifteenMinuteRate.put(nodeId, BigDecimal.valueOf(point.getValue()));
    }
  }

  private void updateTopicsIOrates(String name, GaugeSnapshot.GaugeDataPointSnapshot point) {
    Labels labels = point.getLabels();
    if (labels.contains("topic")
        && containsIgnoreCase(name, "BrokerTopicMetrics")
        && endsWithIgnoreCase(name, "FifteenMinuteRate")) {
      String topic = labels.get("topic");
      if (labels.contains("name")) {
        var nameLblVal = labels.get("name");
        if ("BytesInPerSec".equalsIgnoreCase(nameLblVal)) {
          BigDecimal val = BigDecimal.valueOf(point.getValue());
          bytesInFifteenMinuteRate.merge(topic, val, BigDecimal::add);
        } else if ("BytesOutPerSec".equalsIgnoreCase(nameLblVal)) {
          BigDecimal val = BigDecimal.valueOf(point.getValue());
          bytesOutFifteenMinuteRate.merge(topic, val, BigDecimal::add);
        }
      }
    }
  }

}
