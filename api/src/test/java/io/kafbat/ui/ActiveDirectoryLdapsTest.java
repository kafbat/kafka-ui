package io.kafbat.ui;

import static io.kafbat.ui.container.ActiveDirectoryContainer.CONTAINER_CERT_PATH;
import static io.kafbat.ui.container.ActiveDirectoryContainer.CONTAINER_KEY_PATH;
import static io.kafbat.ui.container.ActiveDirectoryContainer.DOMAIN;
import static io.kafbat.ui.container.ActiveDirectoryContainer.PASSWORD;
import static org.testcontainers.utility.MountableFile.forHostPath;

import io.kafbat.ui.container.ActiveDirectoryContainer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.test.TestSslUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemWriter;

@ContextConfiguration(initializers = {ActiveDirectoryLdapsTest.Initializer.class})
public class ActiveDirectoryLdapsTest extends AbstractActiveDirectoryIntegrationTest {
  private static final ActiveDirectoryContainer ACTIVE_DIRECTORY = new ActiveDirectoryContainer(true);

  private static File certPem = null;
  private static File privateKeyPem = null;

  @BeforeAll
  public static void setup() throws Exception {
    generateCerts();

    ACTIVE_DIRECTORY.withCopyFileToContainer(forHostPath(certPem.getAbsolutePath()), CONTAINER_CERT_PATH);
    ACTIVE_DIRECTORY.withCopyFileToContainer(forHostPath(privateKeyPem.getAbsolutePath()), CONTAINER_KEY_PATH);

    ACTIVE_DIRECTORY.start();
  }

  @AfterAll
  public static void shutdown() {
    ACTIVE_DIRECTORY.stop();
  }

  private static void generateCerts() throws Exception {
    File truststore = File.createTempFile("truststore", ".jks");

    truststore.deleteOnExit();

    String host = "localhost";
    KeyPair clientKeyPair = TestSslUtils.generateKeyPair("RSA");

    X509Certificate clientCert = new TestSslUtils.CertificateBuilder(365, "SHA256withRSA")
        .sanDnsNames(host)
        .sanIpAddress(InetAddress.getByName(host))
        .generate("O=Samba Administration, OU=Samba, CN=" + host, clientKeyPair);

    TestSslUtils.createTrustStore(truststore.getPath(), new Password(PASSWORD), Map.of("client", clientCert));

    certPem = File.createTempFile("cert", ".pem");
    try (FileWriter fw = new FileWriter(certPem)) {
      fw.write(certOrKeyToString(clientCert));
    }

    privateKeyPem = File.createTempFile("key", ".pem");
    try (FileWriter fw = new FileWriter(privateKeyPem)) {
      fw.write(certOrKeyToString(clientKeyPair.getPrivate()));
    }
  }

  private static String certOrKeyToString(Object certOrKey) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      if (certOrKey instanceof X509Certificate) {
        pemWriter.writeObject(new JcaMiscPEMGenerator(certOrKey));
      } else {
        pemWriter.writeObject(new JcaPKCS8Generator((PrivateKey) certOrKey, null));
      }
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
      System.setProperty("spring.ldap.urls", ACTIVE_DIRECTORY.getLdapUrl());
      System.setProperty("oauth2.ldap.activeDirectory", "true");
      System.setProperty("oauth2.ldap.activeDirectory.domain", DOMAIN);
    }
  }
}
