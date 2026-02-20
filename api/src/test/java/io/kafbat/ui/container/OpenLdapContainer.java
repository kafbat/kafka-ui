package io.kafbat.ui.container;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class OpenLdapContainer extends GenericContainer<OpenLdapContainer> {
  public static final String ADMIN_PASSWORD = "StrongPassword123";
  public static final String DOMAIN = "kafbat.io";
  private static final String DOMAIN_DC = "dc=kafbat,dc=io";
  private static final int LDAP_PORT = 1389;
  private static final DockerImageName IMAGE_NAME = DockerImageName.parse("bitnami/openldap:2.6.9");

  public OpenLdapContainer() {
    super(IMAGE_NAME);

    withExposedPorts(LDAP_PORT);

    withEnv("LDAP_ORGANISATION", DOMAIN.replace(".", ""));
    withEnv("LDAP_DOMAIN", DOMAIN);
    withEnv("LDAP_ROOT", DOMAIN_DC);
    withEnv("LDAP_ADMIN_DN", "cn=admin," + DOMAIN_DC);
    withEnv("LDAP_ADMIN_PASSWORD", ADMIN_PASSWORD);
    withEnv("LDAP_LOGLEVEL", "512");

    withCopyFileToContainer(MountableFile.forClasspathResource("/open-ldap/"), "/ldifs/");
    waitingFor(Wait.forLogMessage(".*slapd starting.*", 1));
  }

  public String getLdapUrl() {
    return String.format("ldap://%s:%s", getHost(), getMappedPort(LDAP_PORT));
  }
}
