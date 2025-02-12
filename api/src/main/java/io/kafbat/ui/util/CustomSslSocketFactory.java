package io.kafbat.ui.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CustomSslSocketFactory extends SSLSocketFactory {
  private final SSLSocketFactory socketFactory;

  public CustomSslSocketFactory() {
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[] { new DisabledX509TrustManager() }, new SecureRandom());
      socketFactory = ctx.getSocketFactory();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static SocketFactory getDefault() {
    return new CustomSslSocketFactory();
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return socketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return socketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
    return socketFactory.createSocket(socket, string, i, bln);
  }

  @Override
  public Socket createSocket(String string, int i) throws IOException {
    return socketFactory.createSocket(string, i);
  }

  @Override
  public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException {
    return socketFactory.createSocket(string, i, ia, i1);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i) throws IOException {
    return socketFactory.createSocket(ia, i);
  }

  @Override
  public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
    return socketFactory.createSocket(ia, i, ia1, i1);
  }

  @Override
  public Socket createSocket() throws IOException {
    return socketFactory.createSocket();
  }

  private static class DisabledX509TrustManager implements X509TrustManager {
    /** Empty certificate array. */
    private static final X509Certificate[] CERTS = new X509Certificate[0];

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
      // No-op, all clients are trusted.
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
      // No-op, all servers are trusted.
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return CERTS;
    }
  }
}
