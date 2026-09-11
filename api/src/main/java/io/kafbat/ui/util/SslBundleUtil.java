package io.kafbat.ui.util;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.config.ClustersProperties.StoreType;
import io.kafbat.ui.exception.ValidationException;
import javax.annotation.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;

public class SslBundleUtil {
  public static SslBundle loadBundle(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                     @Nullable ClustersProperties.KeystoreConfig keystoreConfig) {
    if (truststoreConfig == null && keystoreConfig == null) {
      return null;
    }

    StoreType type = null;
    if (truststoreConfig != null && truststoreConfig.getTruststoreType() != null) {
      type = truststoreConfig.getTruststoreType();
    }

    if (keystoreConfig != null && keystoreConfig.getKeystoreType() != null) {
      if (type != null && type != keystoreConfig.getKeystoreType()) {
        throw new ValidationException("Truststore/keystore types must match");
      }

      type = keystoreConfig.getKeystoreType();
    }

    if (type == null) {
      type = StoreType.JKS;
    }

    return switch (type) {
      case JKS, PKCS12 -> loadJksBundle(truststoreConfig, keystoreConfig, type);
      case PEM -> loadPemBundle(truststoreConfig, keystoreConfig);
    };
  }

  public static SslBundle loadPemBundle(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                        @Nullable ClustersProperties.KeystoreConfig keystoreConfig) {
    PemSslStore keyStore = null;
    PemSslStore trustStore = null;
    if (keystoreConfig != null && keystoreConfig.getKeystoreLocation() != null) {
      keyStore = PemSslStore.load(
          new PemSslStoreDetails(
              null,
              null,
              null,
              keystoreConfig.getKeystoreCertificate(),
              keystoreConfig.getKeystoreLocation(),
              keystoreConfig.getKeystorePassword()
          )
      );
    }

    if (truststoreConfig != null && truststoreConfig.getTruststoreLocation() != null) {
      trustStore = PemSslStore.load(
          new PemSslStoreDetails(
              null,
              truststoreConfig.getTruststoreLocation(),
              null
          )
      );
    }

    return SslBundle.of(new PemSslStoreBundle(keyStore, trustStore));
  }

  public static SslBundle loadJksBundle(@Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                        @Nullable ClustersProperties.KeystoreConfig keystoreConfig,
                                        StoreType type) {
    JksSslStoreDetails keyStore = null;
    JksSslStoreDetails trustStore = null;
    if (keystoreConfig != null && keystoreConfig.getKeystoreLocation() != null) {
      keyStore = new JksSslStoreDetails(
          type.name(),
          null,
          keystoreConfig.getKeystoreLocation(),
          keystoreConfig.getKeystorePassword()
      );
    }

    if (truststoreConfig != null && truststoreConfig.getTruststoreLocation() != null) {
      trustStore = new JksSslStoreDetails(
          type.name(),
          null,
          truststoreConfig.getTruststoreLocation(),
          truststoreConfig.getTruststorePassword()
      );
    }

    return SslBundle.of(new JksSslStoreBundle(keyStore, trustStore));
  }
}
