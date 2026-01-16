package io.kafbat.ui.util;

import io.kafbat.ui.api.model.SecurityProtocol;
import io.kafbat.ui.config.ClustersProperties;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.boot.ssl.SslBundle;

public final class KafkaClientSslPropertiesUtil {

  private KafkaClientSslPropertiesUtil() {
  }

  public static void addKafkaSslProperties(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                           @Nullable ClustersProperties.KeystoreConfig keystoreConfig,
                                           @Nullable SecurityProtocol securityProtocol,
                                           Properties sink) {
    if (securityProtocol != null) {
      sink.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol.name());
    }

    if (truststoreConfig != null && !truststoreConfig.isVerify()) {
      sink.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    }

    SslBundle bundle = SslBundleUtil.loadBundle(truststoreConfig, keystoreConfig);
    if (bundle == null) {
      return;
    }

    sink.put(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG, SslBundleSslEngineFactory.class);
    sink.put(SslBundle.class.getName(), bundle);
  }
}
