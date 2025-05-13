package io.kafbat.ui.service.metrics.prometheus;

import static io.kafbat.ui.service.metrics.MetricsUtils.isTheSameMetric;
import static io.kafbat.ui.service.metrics.prometheus.PrometheusExpose.prepareMetricsForGlobalExpose;
import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.model.Metrics;
import io.kafbat.ui.service.metrics.scrape.inferred.InferredMetrics;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrometheusExposeTest {

  @Test
  void prepareMetricsForGlobalExposeAppendsClusterAndBrokerIdLabelsToMetrics() {

    var inferredMfs = new GaugeSnapshot(new MetricMetadata("infer", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(100, Labels.of("lbl1","lblVal1"), null)));

    var broker1Mfs = new GaugeSnapshot(new MetricMetadata("brok", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(101, Labels.of("broklbl1","broklblVal1"), null)));

    var broker2Mfs = new GaugeSnapshot(new MetricMetadata("brok", "help"), List.of(
        new GaugeSnapshot.GaugeDataPointSnapshot(102, Labels.of("broklbl1","broklblVal1"), null)));

    List<MetricSnapshot> prepared = prepareMetricsForGlobalExpose(
        "testCluster",
        Metrics.builder()
            .inferredMetrics(new InferredMetrics(List.of(inferredMfs)))
            .perBrokerScrapedMetrics(Map.of(1, List.of(broker1Mfs), 2, List.of(broker2Mfs)))
            .build()
    ).toList();

    assertThat(prepared)
        .hasSize(3)
        .anyMatch(p -> isTheSameMetric(p, new GaugeSnapshot(new MetricMetadata("infer", "help"), List.of(
            new GaugeSnapshot.GaugeDataPointSnapshot(100,
                Labels.of("cluster", "testCluster", "lbl1", "lblVal1"), null
            ))
        )))
        .anyMatch(p -> isTheSameMetric(p, new GaugeSnapshot(new MetricMetadata("brok", "help"), List.of(
            new GaugeSnapshot.GaugeDataPointSnapshot(101,
                Labels.of(
                    "cluster", "testCluster",
                    "broker_id", "1",
                    "broklbl1","broklblVal1"
                ), null
            ))
        )))
        .anyMatch(p -> isTheSameMetric(p, new GaugeSnapshot(new MetricMetadata("brok", "help"), List.of(
                new GaugeSnapshot.GaugeDataPointSnapshot(102,
                    Labels.of(
                        "cluster", "testCluster",
                        "broker_id", "2",
                        "broklbl1","broklblVal1"
                    ), null
                ))
            )
        ));
  }

}
