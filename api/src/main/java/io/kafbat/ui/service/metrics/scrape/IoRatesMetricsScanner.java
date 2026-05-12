package io.kafbat.ui.service.metrics.scrape;

import static org.apache.commons.lang3.Strings.CI;

import io.kafbat.ui.model.Metrics;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Scans external jmx/prometheus metric and tries to infer io rates
class IoRatesMetricsScanner {

  public static final String BROKER_TOPIC_METRICS_SUFFIX = "BrokerTopicMetrics";
  public static final String FIFTEEN_MINUTE_RATE_SUFFIX = "FifteenMinuteRate";
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
          gauge.getDataPoints().forEach(dp -> {
            updateBrokerIOrates(nodeId, name, dp.getLabels(), dp.getValue());
            updateTopicsIOrates(name, dp.getLabels(), dp.getValue());
          });
        } else if (metric instanceof UnknownSnapshot unknown) {
          unknown.getDataPoints().forEach(dp -> {
            updateBrokerIOrates(nodeId, name, dp.getLabels(), dp.getValue());
            updateTopicsIOrates(name, dp.getLabels(), dp.getValue());
          });
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

  private void updateBrokerIOrates(int nodeId, String name, Labels labels, double value) {
    if (!brokerBytesInFifteenMinuteRate.containsKey(nodeId)
        && labels.size() == 1
        && "BytesInPerSec".equalsIgnoreCase(labels.getValue(0))
        && CI.contains(name, BROKER_TOPIC_METRICS_SUFFIX)
        && CI.endsWith(name, FIFTEEN_MINUTE_RATE_SUFFIX)) {
      brokerBytesInFifteenMinuteRate.put(nodeId, BigDecimal.valueOf(value));
    }
    if (!brokerBytesOutFifteenMinuteRate.containsKey(nodeId)
        && labels.size() == 1
        && "BytesOutPerSec".equalsIgnoreCase(labels.getValue(0))
        && CI.contains(name, BROKER_TOPIC_METRICS_SUFFIX)
        && CI.endsWith(name, FIFTEEN_MINUTE_RATE_SUFFIX)) {
      brokerBytesOutFifteenMinuteRate.put(nodeId, BigDecimal.valueOf(value));
    }
  }

  private void updateTopicsIOrates(String name, Labels labels, double value) {
    if (labels.contains("topic")
        && CI.contains(name, BROKER_TOPIC_METRICS_SUFFIX)
        && CI.endsWith(name, FIFTEEN_MINUTE_RATE_SUFFIX)) {
      String topic = labels.get("topic");
      if (labels.contains("name")) {
        var nameLblVal = labels.get("name");
        if ("BytesInPerSec".equalsIgnoreCase(nameLblVal)) {
          BigDecimal val = BigDecimal.valueOf(value);
          bytesInFifteenMinuteRate.merge(topic, val, BigDecimal::add);
        } else if ("BytesOutPerSec".equalsIgnoreCase(nameLblVal)) {
          BigDecimal val = BigDecimal.valueOf(value);
          bytesOutFifteenMinuteRate.merge(topic, val, BigDecimal::add);
        }
      }
    }
  }

}
