package io.kafbat.ui.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.junit.jupiter.api.Test;

class ReactiveAdminClientVersionTest {

  @Test
  void extractKafkaVersionUsesInterBrokerProtocolVersion() {
    var configs = List.of(
        new ConfigEntry("log.message.format.version", "3.8-IV0"),
        new ConfigEntry("inter.broker.protocol.version", "3.9-IV0")
    );

    assertThat(ReactiveAdminClient.extractKafkaVersion(configs)).contains("3.9-IV0");
  }

  @Test
  void extractKafkaVersionIgnoresLogMessageFormatVersion() {
    var configs = List.of(
        new ConfigEntry("log.message.format.version", "3.8-IV0")
    );

    assertThat(ReactiveAdminClient.extractKafkaVersion(configs)).isEmpty();
  }

  @Test
  void extractKafkaVersionReturnsEmptyWhenInterBrokerProtocolVersionIsNull() {
    var configs = List.of(
        new ConfigEntry("inter.broker.protocol.version", null),
        new ConfigEntry("log.message.format.version", "3.8-IV0")
    );

    assertThat(ReactiveAdminClient.extractKafkaVersion(configs)).isEmpty();
  }

  @Test
  void extractKafkaVersionReturnsEmptyWhenVersionConfigsAreMissing() {
    var configs = List.of(new ConfigEntry("delete.topic.enable", "true"));

    assertThat(ReactiveAdminClient.extractKafkaVersion(configs)).isEmpty();
  }
}
