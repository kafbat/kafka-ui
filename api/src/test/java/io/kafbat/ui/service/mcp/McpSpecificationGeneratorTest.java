package io.kafbat.ui.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.controller.TopicsController;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.KafkaCluster;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.TopicColumnsToSortDTO;
import io.kafbat.ui.model.TopicUpdateDTO;
import io.kafbat.ui.service.ClustersStorage;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.TopicsService;
import io.kafbat.ui.service.acl.AclsService;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

class McpSpecificationGeneratorTest {
  private static final SchemaGenerator SCHEMA_GENERATOR = schemaGenerator();

  private static SchemaGenerator schemaGenerator() {
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    return new SchemaGenerator(configBuilder.build());
  }

  private static McpSpecificationGenerator generatorWith(ClustersStorage storage) {
    return new McpSpecificationGenerator(SCHEMA_GENERATOR, new ObjectMapper(), storage);
  }

  private static TopicsController topicsController() {
    return new TopicsController(
        mock(TopicsService.class), mock(TopicAnalysisService.class), mock(ClusterMapper.class),
        mock(ClustersProperties.class), mock(KafkaConnectService.class), mock(AclsService.class)
    );
  }

  @Test
  void testConvertController() {
    McpSpecificationGenerator generator = generatorWith(mock(ClustersStorage.class));
    List<AsyncToolSpecification> specifications =
        generator.convertTool(topicsController());

    assertThat(specifications).hasSize(17);
    List<McpSchema.Tool> tools = List.of(
        new McpSchema.Tool(
            "recreateTopic",
            "recreateTopic",
            new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName"), false, null, null)
        ),
        new McpSchema.Tool(
            "getTopicConfigs",
            "getTopicConfigs",
            new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName"), false, null, null)
        ),
        new McpSchema.Tool(
            "cloneTopic",
            "cloneTopic",
            new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string"),
                "newTopicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName", "newTopicName"), false, null, null)
        ),
        new McpSchema.Tool(
            "getTopics",
            "getTopics",
            new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "page", Map.of("type", "integer"),
                "perPage", Map.of("type", "integer"),
                "showInternal", Map.of("type", "boolean"),
                "search", Map.of("type", "string"),
                "orderBy", SCHEMA_GENERATOR.generateSchema(TopicColumnsToSortDTO.class),
                "sortOrder", SCHEMA_GENERATOR.generateSchema(SortOrderDTO.class),
                "fts", Map.of("type", "boolean")
            ), List.of("clusterName"), false, null, null)
        ),
        new McpSchema.Tool(
            "updateTopic",
            "updateTopic",
            new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string"),
                "topicUpdate", SCHEMA_GENERATOR.generateSchema(TopicUpdateDTO.class)
            ), List.of("clusterName", "topicName", "topicUpdate"), false, null, null)
        )
    );
    assertThat(tools).allMatch(tool ->
        specifications.stream().anyMatch(s -> s.tool().equals(tool))
    );
  }

  @Test
  void writeOperationBlockedOnReadOnlyCluster() {
    ClustersStorage storage = readOnlyClusterStorage();
    McpSpecificationGenerator generator = generatorWith(storage);
    List<AsyncToolSpecification> specs = generator.convertTool(topicsController());

    AsyncToolSpecification createTopic = findTool(specs, "createTopic");

    Mono<CallToolResult> result = invokeTool(createTopic, Map.of("clusterName", "readonly-cluster"));

    StepVerifier.create(result)
        .assertNext(callResult -> {
          assertThat(callResult.isError()).isTrue();
          assertThat(callResult.content()).isNotEmpty();
          String text = ((McpSchema.TextContent) callResult.content().get(0)).text();
          assertThat(text).contains("read-only");
        })
        .verifyComplete();
  }

  @Test
  void writeOperationNotBlockedOnNonReadOnlyCluster() {
    KafkaCluster normalCluster = KafkaCluster.builder()
        .name("normal-cluster")
        .readOnly(false)
        .build();
    ClustersStorage storage = mock(ClustersStorage.class);
    when(storage.getClusterByName(eq("normal-cluster")))
        .thenReturn(Optional.of(normalCluster));

    McpSpecificationGenerator generator = generatorWith(storage);
    List<AsyncToolSpecification> specs = generator.convertTool(topicsController());

    AsyncToolSpecification createTopic = findTool(specs, "createTopic");

    Mono<CallToolResult> result = invokeTool(createTopic, Map.of("clusterName", "normal-cluster"));

    StepVerifier.create(result)
        .assertNext(callResult -> {
          assertThat(callResult.isError()).isTrue();
          String text = ((McpSchema.TextContent) callResult.content().get(0)).text();
          if (text != null) {
            assertThat(text).doesNotContain("read-only");
          }
        })
        .verifyComplete();
  }

  @Test
  void readOperationAllowedOnReadOnlyCluster() {
    ClustersStorage storage = readOnlyClusterStorage();
    McpSpecificationGenerator generator = generatorWith(storage);
    List<AsyncToolSpecification> specs = generator.convertTool(topicsController());

    // getTopics is a GET operation — should NOT be blocked on readOnly cluster
    AsyncToolSpecification getTopics = findTool(specs, "getTopics");

    Mono<CallToolResult> result = invokeTool(getTopics, Map.of("clusterName", "readonly-cluster"));

    StepVerifier.create(result)
        .assertNext(callResult -> {
          // May error from unmocked dependencies, but NOT from readOnly check
          if (callResult.isError()) {
            String text = ((McpSchema.TextContent) callResult.content().get(0)).text();
            if (text != null) {
              assertThat(text).doesNotContain("read-only");
            }
          }
        })
        .verifyComplete();
  }

  @Test
  void safeWriteOperationAllowedOnReadOnlyCluster() {
    ClustersStorage storage = readOnlyClusterStorage();
    McpSpecificationGenerator generator = generatorWith(storage);
    List<AsyncToolSpecification> specs = generator.convertTool(topicsController());

    // analyzeTopic is POST but listed in READ_ONLY_SAFE_OPERATIONS
    AsyncToolSpecification analyzeTopic = findTool(specs, "analyzeTopic");

    Mono<CallToolResult> result = invokeTool(analyzeTopic, Map.of(
        "clusterName", "readonly-cluster", "topicName", "test-topic"));

    StepVerifier.create(result)
        .assertNext(callResult -> {
          if (callResult.isError()) {
            String text = ((McpSchema.TextContent) callResult.content().get(0)).text();
            if (text != null) {
              assertThat(text).doesNotContain("read-only");
            }
          }
        })
        .verifyComplete();
  }

  @Test
  void writeOperationWithUnknownClusterProceeds() {
    // Unknown cluster returns Optional.empty() — orElse(false) means not readOnly
    ClustersStorage storage = mock(ClustersStorage.class);
    when(storage.getClusterByName(eq("unknown-cluster")))
        .thenReturn(Optional.empty());

    McpSpecificationGenerator generator = generatorWith(storage);
    List<AsyncToolSpecification> specs = generator.convertTool(topicsController());

    AsyncToolSpecification createTopic = findTool(specs, "createTopic");

    Mono<CallToolResult> result = invokeTool(createTopic, Map.of("clusterName", "unknown-cluster"));

    StepVerifier.create(result)
        .assertNext(callResult -> {
          // Should proceed past readOnly check (unknown cluster defaults to non-readOnly)
          if (callResult.isError()) {
            String text = ((McpSchema.TextContent) callResult.content().get(0)).text();
            if (text != null) {
              assertThat(text).doesNotContain("read-only");
            }
          }
        })
        .verifyComplete();
  }

  // --- helpers ---

  private static ClustersStorage readOnlyClusterStorage() {
    KafkaCluster readOnlyCluster = KafkaCluster.builder()
        .name("readonly-cluster")
        .readOnly(true)
        .build();
    ClustersStorage storage = mock(ClustersStorage.class);
    when(storage.getClusterByName(eq("readonly-cluster")))
        .thenReturn(Optional.of(readOnlyCluster));
    return storage;
  }

  private static AsyncToolSpecification findTool(List<AsyncToolSpecification> specs, String name) {
    return specs.stream()
        .filter(s -> s.tool().name().equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Tool not found: " + name));
  }

  private static Mono<CallToolResult> invokeTool(
      AsyncToolSpecification spec, Map<String, Object> args) {
    McpAsyncServerExchange mcpExchange = mock(McpAsyncServerExchange.class);
    ServerWebExchange webExchange = mock(ServerWebExchange.class);
    return spec.call().apply(mcpExchange, new HashMap<>(args))
        .contextWrite(Context.of(ServerWebExchange.class, webExchange));
  }
}
