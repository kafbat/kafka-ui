package io.kafbat.ui;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.kafbat.ui.config.ClustersProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
//@EnableConfigurationProperties(ClustersProperties.class)
@TestPropertySource(properties = {
    "kafka.clusters.0.name=",
})
class ConfigurationTests  {

  @Autowired
  private ClustersProperties clustersProperties;

  @Test
  void shouldFailWithotName() {
    assertThrows(BindException.class, () -> clustersProperties.getClusters());
  }

}
