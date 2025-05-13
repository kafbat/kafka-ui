package io.kafbat.ui.model;

import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;


@Builder
@Value
public class Metrics {

  IoRates ioRates;
  InferredMetrics inferredMetrics;
  Map<Integer, List<MetricSnapshot>> perBrokerScrapedMetrics;

  public static Metrics empty() {
    return Metrics.builder()
        .ioRates(IoRates.empty())
        .perBrokerScrapedMetrics(Map.of())
        .inferredMetrics(InferredMetrics.empty())
        .build();
  }

  @Builder
  public record IoRates(Map<Integer, BigDecimal> brokerBytesInPerSec,
                        Map<Integer, BigDecimal> brokerBytesOutPerSec,
                        Map<String, BigDecimal> topicBytesInPerSec,
                        Map<String, BigDecimal> topicBytesOutPerSec) {

    static IoRates empty() {
      return IoRates.builder()
          .brokerBytesOutPerSec(Map.of())
          .brokerBytesInPerSec(Map.of())
          .topicBytesOutPerSec(Map.of())
          .topicBytesInPerSec(Map.of())
          .build();
    }
  }

}
