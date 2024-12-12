package io.kafbat.ui.service.ksql;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.kafbat.ui.exception.KsqlApiException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.KsqlStreamDescriptionDTO;
import io.kafbat.ui.model.KsqlTableDescriptionDTO;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class KsqlServiceV2 {

  private record KsqlExecuteCommand(KafkaCluster cluster, String ksql, Map<String, String> streamProperties) {
  }

  private final Cache<String, KsqlExecuteCommand> registeredCommands =
      CacheBuilder.newBuilder()
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build();

  public String registerCommand(KafkaCluster cluster,
                                String ksql,
                                Map<String, String> streamProperties) {
    String uuid = UUID.randomUUID().toString();
    registeredCommands.put(uuid, new KsqlExecuteCommand(cluster, ksql, streamProperties));
    return uuid;
  }

  public Flux<KsqlApiClient.KsqlResponseTable> execute(String commandId) {
    var cmd = registeredCommands.getIfPresent(commandId);
    if (cmd == null) {
      throw new ValidationException("No command registered with id " + commandId);
    }
    registeredCommands.invalidate(commandId);
    return cmd.cluster.getKsqlClient()
        .flux(client -> client.execute(cmd.ksql, cmd.streamProperties));
  }

  public Flux<KsqlTableDescriptionDTO> listTables(KafkaCluster cluster) {
    return cluster.getKsqlClient()
        .flux(client -> client.execute("LIST TABLES;", Map.of()))
        .flatMap(resp -> {
          if (!resp.getHeader().equals("Tables")) {
            log.error("Unexpected result header: {}", resp.getHeader());
            log.debug("Unexpected result {}", resp);
            return Flux.error(new KsqlApiException("Error retrieving tables list"));
          }
          return Flux.fromIterable(resp.getValues()
              .stream()
              .map(row ->
                  new KsqlTableDescriptionDTO()
                      .name(resp.getColumnValue(row, "name").map(JsonNode::asText).orElse(null))
                      .topic(resp.getColumnValue(row, "topic").map(JsonNode::asText).orElse(null))
                      .keyFormat(resp.getColumnValue(row, "keyFormat").map(JsonNode::asText).orElse(null))
                      .valueFormat(resp.getColumnValue(row, "valueFormat").map(JsonNode::asText).orElse(null))
                      .isWindowed(resp.getColumnValue(row, "isWindowed").map(JsonNode::asBoolean).orElse(null)))
              .collect(Collectors.toList()));
        });
  }

  public Flux<KsqlStreamDescriptionDTO> listStreams(KafkaCluster cluster) {
    return cluster.getKsqlClient()
        .flux(client -> client.execute("LIST STREAMS;", Map.of()))
        .flatMap(resp -> {
          if (!resp.getHeader().equals("Streams")) {
            log.error("Unexpected result header: {}", resp.getHeader());
            log.debug("Unexpected result {}", resp);
            return Flux.error(new KsqlApiException("Error retrieving streams list"));
          }
          return Flux.fromIterable(resp.getValues()
              .stream()
              .map(row ->
                  new KsqlStreamDescriptionDTO()
                      .name(resp.getColumnValue(row, "name").map(JsonNode::asText).orElse(null))
                      .topic(resp.getColumnValue(row, "topic").map(JsonNode::asText).orElse(null))
                      .keyFormat(resp.getColumnValue(row, "keyFormat").map(JsonNode::asText).orElse(null))
                      .valueFormat(
                          // for old versions (<0.13) "format" column is filled,
                          // for new version "keyFormat" & "valueFormat" columns should be filled
                          resp.getColumnValue(row, "valueFormat")
                              .or(() -> resp.getColumnValue(row, "format"))
                              .map(JsonNode::asText)
                              .orElse(null))
              )
              .collect(Collectors.toList()));
        });
  }

}
