package io.kafbat.ui.container;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class ActiveDirectoryContainer extends GenericContainer<ActiveDirectoryContainer> {
  public static final DockerImageName IMAGE_NAME = DockerImageName.parse("nowsci/samba-domain:latest");
  public static final String DOMAIN = "corp.kafbat.io";
  public static final String PASSWORD = "StrongPassword123";
  public static final String FIRST_GROUP_USER = "JohnDoe";
  public static final String SECOND_GROUP_USER = "JohnWick";
  public static final String USER_WITHOUT_GROUP = "JohnJames";

  private static final String DOMAIN_DC = "dc=corp,dc=kafbat,dc=io";
  private static final String GROUP = "group";
  private static final String FIRST_GROUP = "firstGroup";
  private static final String SECOND_GROUP = "secondGroup";
  private static final String DOMAIN_EMAIL = "kafbat.io";
  private static final String SAMBA_TOOL = "samba-tool";
  private static final int LDAP_PORT = 389;

  public ActiveDirectoryContainer() {
    super(IMAGE_NAME);

    withExposedPorts(LDAP_PORT);

    withEnv("DOMAIN", DOMAIN);
    withEnv("DOMAIN_DC", DOMAIN_DC);
    withEnv("DOMAIN_EMAIL", DOMAIN_EMAIL);
    withEnv("DOMAINPASS", PASSWORD);
    withEnv("NOCOMPLEXITY", "true");
    withEnv("INSECURELDAP", "true");

    withPrivilegedMode(true);
  }

  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    createUser(USER_WITHOUT_GROUP);
    createUser(FIRST_GROUP_USER);
    createUser(SECOND_GROUP_USER);

    exec(SAMBA_TOOL, GROUP, "add", FIRST_GROUP);
    exec(SAMBA_TOOL, GROUP, "add", SECOND_GROUP);

    exec(SAMBA_TOOL, GROUP, "addmembers", FIRST_GROUP, FIRST_GROUP_USER);
    exec(SAMBA_TOOL, GROUP, "addmembers", SECOND_GROUP, SECOND_GROUP_USER);
  }

  public String getLdapUrl() {
    return String.format("ldap://%s:%s", getHost(), getMappedPort(LDAP_PORT));
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
}
