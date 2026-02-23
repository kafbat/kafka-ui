package io.kafbat.ui.service.audit;

import static io.kafbat.ui.AbstractActiveDirectoryIntegrationTest.SESSION;
import static io.kafbat.ui.AbstractActiveDirectoryIntegrationTest.session;
import static io.kafbat.ui.AbstractIntegrationTest.KAFKA_IMAGE_NAME;
import static io.kafbat.ui.AbstractIntegrationTest.LOCAL;
import static io.kafbat.ui.container.ActiveDirectoryContainer.DOMAIN;
import static io.kafbat.ui.container.ActiveDirectoryContainer.FIRST_USER_WITH_GROUP;
import static io.kafbat.ui.container.ActiveDirectoryContainer.SECOND_USER_WITH_GROUP;
import static io.kafbat.ui.service.audit.AuditService.DEFAULT_AUDIT_TOPIC_NAME;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.kafbat.ui.container.ActiveDirectoryContainer;
import io.kafbat.ui.model.TopicCreationDTO;
import io.kafbat.ui.model.TopicDetailsDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.model.TopicMessageEventDTO;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.kafka.ConfluentKafkaContainer;

@SpringBootTest
@ActiveProfiles("rbac-audit")
@AutoConfigureWebTestClient(timeout = "60000")
@ContextConfiguration(initializers = {AuditRbacIntegrationTest.Initializer.class})
public class AuditRbacIntegrationTest {
  private static final ActiveDirectoryContainer ACTIVE_DIRECTORY = new ActiveDirectoryContainer(false);

  private static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(KAFKA_IMAGE_NAME);

  @Autowired
  private WebTestClient webTestClient;

  @BeforeAll
  public static void setup() {
    ACTIVE_DIRECTORY.start();
    KAFKA.start();
  }

  @AfterAll
  public static void shutdown() {
    KAFKA.stop();
    ACTIVE_DIRECTORY.stop();
  }

  @Test
  void testAccessForAuditTopic() {
    String topicName = "test_audit_" + UUID.randomUUID();

    webTestClient.post()
        .uri("/api/clusters/{clusterName}/topics", LOCAL)
        .cookie(SESSION, session(webTestClient, FIRST_USER_WITH_GROUP))
        .bodyValue(new TopicCreationDTO().replicationFactor(1).partitions(1).name(topicName))
        .exchange()
        .expectStatus()
        .isOk();

    String viewerSession = session(webTestClient, SECOND_USER_WITH_GROUP);

    TopicDetailsDTO details = webTestClient.get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}", LOCAL, DEFAULT_AUDIT_TOPIC_NAME)
        .cookie(SESSION, viewerSession)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(TopicDetailsDTO.class)
        .returnResult()
        .getResponseBody();

    assertNotNull(details);
    assertTrue(details.getPartitionCount() != null && details.getPartitionCount() == 1);

    List<TopicMessageEventDTO> events = webTestClient.get()
        .uri("/api/clusters/{clusterName}/topics/{topicName}/messages/v2", LOCAL, DEFAULT_AUDIT_TOPIC_NAME)
        .cookie(SESSION, viewerSession)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(TopicMessageEventDTO.class)
        .returnResult()
        .getResponseBody();

    assertNotNull(events);
    assertTrue(events.stream().anyMatch(dto -> {
      TopicMessageDTO message = dto.getMessage();

      if (message != null && message.getValue() != null) {
        return message.getValue().contains("\"type\":\"TOPIC\",\"id\":\"" + topicName);
      }

      return false;
    }));
  }

  public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
      System.setProperty("kafka.clusters.0.name", LOCAL);
      System.setProperty("kafka.clusters.0.bootstrapServers", KAFKA.getBootstrapServers());
      System.setProperty("kafka.clusters.0.audit.topicAuditEnabled", "true");
      System.setProperty("spring.ldap.urls", ACTIVE_DIRECTORY.getLdapUrl());
      System.setProperty("oauth2.ldap.activeDirectory", "true");
      System.setProperty("oauth2.ldap.activeDirectory.domain", DOMAIN);
    }
  }
}
