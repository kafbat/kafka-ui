package io.kafbat.ui.container;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class ActiveDirectoryContainer extends GenericContainer<ActiveDirectoryContainer> {
  public static final String DOMAIN = "corp.kafbat.io";
  public static final String PASSWORD = "StrongPassword123";
  public static final String FIRST_USER_WITH_GROUP = "JohnDoe";
  public static final String SECOND_USER_WITH_GROUP = "JohnWick";
  public static final String USER_WITHOUT_GROUP = "JackSmith";
  public static final String EMPTY_PERMISSIONS_USER = "JohnJames";
  public static final String CONTAINER_CERT_PATH = "/var/containers/cert.pem";
  public static final String CONTAINER_KEY_PATH = "/var/containers/key.pem";

  private static final String DOMAIN_DC = "dc=corp,dc=kafbat,dc=io";
  private static final String GROUP = "group";
  private static final String FIRST_GROUP = "firstGroup";
  private static final String SECOND_GROUP = "secondGroup";
  private static final String DOMAIN_EMAIL = "kafbat.io";
  private static final String SAMBA_TOOL = "samba-tool";
  private static final int LDAP_PORT = 389;
  private static final int LDAPS_PORT = 636;
  private static final DockerImageName IMAGE_NAME = DockerImageName.parse("nowsci/samba-domain:latest");

  private final boolean sslEnabled;
  private final int port;

  public ActiveDirectoryContainer(boolean sslEnabled) {
    super(IMAGE_NAME);

    this.sslEnabled = sslEnabled;
    port = sslEnabled ? LDAPS_PORT : LDAP_PORT;

    withExposedPorts(port);

    withEnv("DOMAIN", DOMAIN);
    withEnv("DOMAIN_DC", DOMAIN_DC);
    withEnv("DOMAIN_EMAIL", DOMAIN_EMAIL);
    withEnv("DOMAINPASS", PASSWORD);
    withEnv("NOCOMPLEXITY", "true");
    withEnv("INSECURELDAP", String.valueOf(!sslEnabled));

    withPrivilegedMode(true);
  }

  @Override
  public void start() {
    super.start();

    if (sslEnabled) {
      setCustomCertAndRestartServer();
    }
  }

  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    createUser(EMPTY_PERMISSIONS_USER);
    createUser(USER_WITHOUT_GROUP);
    createUser(FIRST_USER_WITH_GROUP);
    createUser(SECOND_USER_WITH_GROUP);

    exec(SAMBA_TOOL, GROUP, "add", FIRST_GROUP);
    exec(SAMBA_TOOL, GROUP, "add", SECOND_GROUP);
    exec(SAMBA_TOOL, GROUP, "addmembers", FIRST_GROUP, FIRST_USER_WITH_GROUP);
    exec(SAMBA_TOOL, GROUP, "addmembers", SECOND_GROUP, SECOND_USER_WITH_GROUP);
  }

  public String getLdapUrl() {
    return String.format("%s://%s:%s", sslEnabled ? "ldaps" : "ldap", getHost(), getMappedPort(port));
  }

  private void createUser(String name) {
    exec(SAMBA_TOOL, "user", "create", name, PASSWORD, "--mail-address", name + '@' + DOMAIN_EMAIL);
    exec(SAMBA_TOOL, "user", "setexpiry", name, "--noexpiry");
  }

  private void exec(String... cmd) {
    ExecResult result;
    try {
      result = execInContainer(cmd);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (result.getStdout() != null && !result.getStdout().isEmpty()) {
      log.info("Output: {}", result.getStdout());
    }

    if (result.getExitCode() != 0) {
      throw new IllegalStateException(result.toString());
    }
  }

  private void setCustomCertAndRestartServer() {
    exec(
        "sed",
        "-i",
        "/\\[global\\]/a \\\t"
            + "tls cafile = \\\n"
            + "tls keyfile = " + CONTAINER_KEY_PATH + "\\\n"
            + "tls certfile = " + CONTAINER_CERT_PATH + "\\\n",
        "/etc/samba/external/smb.conf"
    );

    exec("chown", "-R", "root:root", "/var/containers/");
    exec("chmod", "600", CONTAINER_KEY_PATH);

    exec("./domain.sh", "reload-config");
    exec("supervisorctl", "restart", "samba");
  }
}
