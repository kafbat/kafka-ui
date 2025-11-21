package io.kafbat.ui.service.ssl;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.security.KeyStore;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;

@SuppressWarnings("unused")
public class SkipTrustManagerFactorySpi extends javax.net.ssl.TrustManagerFactorySpi {

  public SkipTrustManagerFactorySpi() {
  }

  @Override
  protected void engineInit(KeyStore ks) {
  }

  @Override
  protected void engineInit(ManagerFactoryParameters spec) {
  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return InsecureTrustManagerFactory.INSTANCE.getTrustManagers();
  }
}
