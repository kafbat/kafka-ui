package io.kafbat.ui.container;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KsqlDbContainer extends GenericContainer<KsqlDbContainer> {

  private static final int PORT = 8088;

  public KsqlDbContainer(DockerImageName imageName) {
    super(imageName);
    addExposedPort(PORT);
    waitStrategy = Wait
        .forHttp("/info")
        .forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(5));
  }

  public KsqlDbContainer withKafka(ConfluentKafkaContainer kafka) {
    dependsOn(kafka);
    String bootstrapServers = kafka.getNetworkAliases().get(0) + ":9095";
    return withKafka(kafka.getNetwork(), bootstrapServers);
  }

  private KsqlDbContainer withKafka(Network network, String bootstrapServers) {
    withNetwork(network);
    withEnv("KSQL_LISTENERS", "http://0.0.0.0:" + PORT);
    withEnv("KSQL_BOOTSTRAP_SERVERS", bootstrapServers);
    return self();
  }

  public String url() {
    return "http://" + getHost() + ":" + getMappedPort(PORT);
  }
}
