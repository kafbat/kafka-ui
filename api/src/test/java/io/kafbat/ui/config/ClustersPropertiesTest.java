package io.kafbat.ui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ClustersPropertiesTest {

  @Test
  void clusterNamesShouldBeUniq() {
    ClustersProperties properties = new ClustersProperties();
    var c1 = new ClustersProperties.Cluster();
    c1.setName("test");
    var c2 = new ClustersProperties.Cluster();
    c2.setName("test"); //same name

    Collections.addAll(properties.getClusters(), c1, c2);

    assertThatThrownBy(properties::validateAndSetDefaults)
        .hasMessageContaining("Application config isn't valid");
  }

  @Test
  void clusterNamesShouldSetIfMultipleClustersProvided() {
    ClustersProperties properties = new ClustersProperties();
    var c1 = new ClustersProperties.Cluster();
    c1.setName("test1");
    var c2 = new ClustersProperties.Cluster(); //name not set

    Collections.addAll(properties.getClusters(), c1, c2);

    assertThatThrownBy(properties::validateAndSetDefaults)
        .hasMessageContaining("Application config isn't valid");
  }

  @Test
  void ifOnlyOneClusterProvidedNameIsOptionalAndSetToDefault() {
    ClustersProperties properties = new ClustersProperties();
    properties.getClusters().add(new ClustersProperties.Cluster());

    properties.validateAndSetDefaults();

    assertThat(properties.getClusters())
        .element(0)
        .extracting("name")
        .isEqualTo("Default");
  }

  @Test
  void topicConfigsExpiryDefaultsToZeroSoConfigsAreDescribedEveryScrape() {
    ClustersProperties properties = new ClustersProperties();

    assertThat(properties.getScrape().getTopicConfigsExpiry()).isEqualTo(Duration.ZERO);
    assertThat(properties.resolveTopicConfigsExpiry(new ClustersProperties.Cluster())).isEqualTo(Duration.ZERO);
    assertThat(properties.resolveTopicConfigsExpiry(null)).isEqualTo(Duration.ZERO);
  }

  @Test
  void topicConfigsExpiryFallsBackToGlobalWhenClusterHasNoScrapeSection() {
    ClustersProperties properties = new ClustersProperties();
    properties.getScrape().setTopicConfigsExpiry(Duration.ofMinutes(5));

    assertThat(properties.resolveTopicConfigsExpiry(new ClustersProperties.Cluster()))
        .isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void clusterLevelTopicConfigsExpiryOverridesGlobal() {
    ClustersProperties properties = new ClustersProperties();
    properties.getScrape().setTopicConfigsExpiry(Duration.ofMinutes(5));
    var cluster = new ClustersProperties.Cluster();
    cluster.setScrape(new ClustersProperties.ScrapeProperties(Duration.ofHours(1)));

    assertThat(properties.resolveTopicConfigsExpiry(cluster)).isEqualTo(Duration.ofHours(1));
  }

}
