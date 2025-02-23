package io.kafbat.ui;

import static io.kafbat.ui.container.ActiveDirectoryContainer.CONTAINER_CERT_PATH;
import static io.kafbat.ui.container.ActiveDirectoryContainer.CONTAINER_KEY_PATH;
import static io.kafbat.ui.container.ActiveDirectoryContainer.DOMAIN;
import static io.kafbat.ui.container.ActiveDirectoryContainer.PASSWORD;
import static java.nio.file.Files.writeString;
import static org.testcontainers.utility.MountableFile.forHostPath;

import io.kafbat.ui.container.ActiveDirectoryContainer;
import java.io.File;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.test.TestSslUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.testcontainers.shaded.org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemWriter;

@DirtiesContext
@ContextConfiguration(initializers = {ActiveDirectoryLdapsTest.Initializer.class})
public class ActiveDirectoryLdapsTest extends AbstractActiveDirectoryIntegrationTest {
  private static final ActiveDirectoryContainer ACTIVE_DIRECTORY = new ActiveDirectoryContainer(true);

  private static File certPem = null;
  private static File privateKeyPem = null;

  @Autowired
  private WebTestClient webTestClient;

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

  @Test
  public void testUserPermissions() {
    checkUserPermissions(webTestClient);
  }

  @Test
  public void testEmptyPermissions() {
    checkEmptyPermissions(webTestClient);
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
    writeString(certPem.toPath(), certOrKeyToString(clientCert));

    privateKeyPem = File.createTempFile("key", ".pem");
    writeString(privateKeyPem.toPath(), certOrKeyToString(clientKeyPair.getPrivate()));
  }

  private static String certOrKeyToString(Object certOrKey) throws Exception {
    StringWriter sw = new StringWriter();
    try (PemWriter pw = new PemWriter(sw)) {
      if (certOrKey instanceof X509Certificate) {
        pw.writeObject(new JcaMiscPEMGenerator(certOrKey));
      } else {
        pw.writeObject(new JcaPKCS8Generator((PrivateKey) certOrKey, null));
      }
    }
    return sw.toString();
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
