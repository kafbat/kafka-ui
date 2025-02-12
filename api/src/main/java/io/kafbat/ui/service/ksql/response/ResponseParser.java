package io.kafbat.ui.service.ksql.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.kafbat.ui.exception.KsqlApiException;
import io.kafbat.ui.service.ksql.KsqlApiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ResponseParser {

  private ResponseParser() {
  }

  public static Optional<KsqlApiClient.KsqlResponseTable> parseSelectResponse(JsonNode jsonNode) {
    // in response, we're getting either header record or row data
    if (arrayFieldNonEmpty(jsonNode, "header")) {
      return Optional.of(
          KsqlApiClient.KsqlResponseTable.builder()
              .header("Schema")
              .columnNames(parseSelectHeadersString(jsonNode.get("header").get("schema").asText()))
              .build());
    }
    if (arrayFieldNonEmpty(jsonNode, "row")) {
      return Optional.of(
          KsqlApiClient.KsqlResponseTable.builder()
              .header("Row")
              .values(
                  List.of(Lists.newArrayList(jsonNode.get("row").get("columns"))))
              .build());
    }
    if (jsonNode.hasNonNull("errorMessage")) {
      throw new KsqlApiException("Error: " + jsonNode.get("errorMessage"));
    }
    // remaining events can be skipped
    return Optional.empty();
  }

  @VisibleForTesting
  static List<String> parseSelectHeadersString(String str) {
    List<String> headers = new ArrayList<>();
    int structNesting = 0;
    boolean quotes = false;
    var headerBuilder = new StringBuilder();
    for (char ch : str.toCharArray()) {
      if (ch == '<') {
        structNesting++;
      } else if (ch == '>') {
        structNesting--;
      } else if (ch == '`') {
        quotes = !quotes;
      } else if (ch == ' ' && headerBuilder.isEmpty()) {
        continue; //skipping leading & training whitespaces
      } else if (ch == ',' && structNesting == 0 && !quotes) {
        headers.add(headerBuilder.toString());
        headerBuilder = new StringBuilder();
        continue;
      }
      headerBuilder.append(ch);
    }
    if (!headerBuilder.isEmpty()) {
      headers.add(headerBuilder.toString());
    }
    return headers;
  }

  public static KsqlApiClient.KsqlResponseTable errorTableWithTextMsg(String errorText) {
    return KsqlApiClient.KsqlResponseTable.builder()
        .header("Execution error")
        .columnNames(List.of("message"))
        .values(List.of(List.of(new TextNode(errorText))))
        .error(true)
        .build();
  }

  public static KsqlApiClient.KsqlResponseTable parseErrorResponse(WebClientResponseException e) {
    try {
      var errBody = new JsonMapper().readTree(e.getResponseBodyAsString());
      return DynamicParser.parseObject("Execution error", errBody)
          .toBuilder()
          .error(true)
          .build();
    } catch (Exception ex) {
      return errorTableWithTextMsg(
          String.format(
              "Unparsable error response from ksqdb, status:'%s', body: '%s'",
              e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public static List<KsqlApiClient.KsqlResponseTable> parseStatementResponse(JsonNode jsonNode) {
    var type = Optional.ofNullable(jsonNode.get("@type"))
        .map(JsonNode::asText)
        .orElse("unknown");

    // messages structure can be inferred from https://github.com/confluentinc/ksql/blob/master/ksqldb-rest-model/src/main/java/io/confluent/ksql/rest/entity/KsqlEntity.java
    return switch (type) {
      case "currentStatus" -> parseObject(
          "Status",
          List.of("status", "message"),
          jsonNode.get("commandStatus")
      );
      case "properties" -> parseProperties(jsonNode);
      case "queries" -> parseArray("Queries", "queries", jsonNode);
      case "sourceDescription" -> parseObjectDynamically("Source Description", jsonNode.get("sourceDescription"));
      case "queryDescription" -> parseObjectDynamically("Queries Description", jsonNode.get("queryDescription"));
      case "topicDescription" -> parseObject(
          "Topic Description",
          List.of("name", "kafkaTopic", "format", "schemaString"),
          jsonNode
      );
      case "streams" -> parseArray("Streams", "streams", jsonNode);
      case "tables" -> parseArray("Tables", "tables", jsonNode);
      case "kafka_topics" -> parseArray("Topics", "topics", jsonNode);
      case "kafka_topics_extended" -> parseArray("Topics extended", "topics", jsonNode);
      case "executionPlan" -> parseObject("Execution plan", List.of("executionPlanText"), jsonNode);
      case "source_descriptions" -> parseArray("Source descriptions", "sourceDescriptions", jsonNode);
      case "query_descriptions" -> parseArray("Queries", "queryDescriptions", jsonNode);
      case "describe_function" -> parseObject(
          "Function description",
          List.of("name", "author", "version", "description", "functions", "path", "type"),
          jsonNode
      );
      case "function_names" -> parseArray("Function Names", "functions", jsonNode);
      case "connector_info" -> parseObjectDynamically("Connector Info", jsonNode.get("info"));
      case "drop_connector" -> parseObject("Dropped connector", List.of("connectorName"), jsonNode);
      case "connector_list" -> parseArray("Connectors", "connectors", jsonNode);
      case "connector_plugins_list" -> parseArray("Connector Plugins", "connectorPlugins", jsonNode);
      case "connector_description" -> parseObject(
          "Connector Description",
          List.of("connectorClass", "status", "sources", "topics"),
          jsonNode
      );
      default -> parseUnknownResponse(jsonNode);
    };
  }

  private static List<KsqlApiClient.KsqlResponseTable> parseObjectDynamically(
      String tableName, JsonNode jsonNode) {
    return List.of(DynamicParser.parseObject(tableName, jsonNode));
  }

  private static List<KsqlApiClient.KsqlResponseTable> parseObject(
      String tableName, List<String> fields, JsonNode jsonNode) {
    return List.of(DynamicParser.parseObject(tableName, fields, jsonNode));
  }

  private static List<KsqlApiClient.KsqlResponseTable> parseArray(
      String tableName, String arrayField, JsonNode jsonNode) {
    return List.of(DynamicParser.parseArray(tableName, jsonNode.get(arrayField)));
  }

  private static List<KsqlApiClient.KsqlResponseTable> parseProperties(JsonNode jsonNode) {
    var tables = new ArrayList<KsqlApiClient.KsqlResponseTable>();
    if (arrayFieldNonEmpty(jsonNode, "properties")) {
      tables.add(DynamicParser.parseArray("properties", jsonNode.get("properties")));
    }
    if (arrayFieldNonEmpty(jsonNode, "overwrittenProperties")) {
      tables.add(DynamicParser.parseArray("overwrittenProperties",
          jsonNode.get("overwrittenProperties")));
    }
    return tables;
  }

  private static List<KsqlApiClient.KsqlResponseTable> parseUnknownResponse(JsonNode jsonNode) {
    return List.of(DynamicParser.parseObject("Ksql Response", jsonNode));
  }

  private static boolean arrayFieldNonEmpty(JsonNode json, String field) {
    return json.hasNonNull(field) && !json.get(field).isEmpty();
  }

}
