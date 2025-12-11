package io.kafbat.ui.util;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Properties;
import jakarta.annotation.Nullable;
import org.apache.kafka.common.config.SslConfigs;

public final class KafkaClientSslPropertiesUtil {

  private KafkaClientSslPropertiesUtil() {
  }

  public static void addKafkaSslProperties(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                           Properties sink) {
    if (truststoreConfig == null) {
      return;
    }

    if (!truststoreConfig.isVerify()) {
      sink.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    }

    if (truststoreConfig.getTruststoreLocation() == null) {
      return;
    }

    sink.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreConfig.getTruststoreLocation());

    if (truststoreConfig.getTruststorePassword() != null) {
      sink.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststoreConfig.getTruststorePassword());
    }

  }
}
