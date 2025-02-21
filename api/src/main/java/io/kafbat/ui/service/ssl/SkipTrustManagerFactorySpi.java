package io.kafbat.ui.service.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SkipTrustManagerFactorySpi extends javax.net.ssl.TrustManagerFactorySpi {

  private final TrustManager[] trustAllCertificates;

  public SkipTrustManagerFactorySpi() {
    this.trustAllCertificates =  new TrustManager[]{
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() { return null; }
          public void checkClientTrusted(X509Certificate[] certs, String authType) { }
          public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        }
    };
  }

  @Override
  protected void engineInit(KeyStore ks) throws KeyStoreException {

  }

  @Override
  protected void engineInit(ManagerFactoryParameters spec)
      throws InvalidAlgorithmParameterException {

  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    return trustAllCertificates;
  }
}
