package io.kafbat.ui.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.controller.TopicsController;
import io.kafbat.ui.mapper.ClusterMapper;
import io.kafbat.ui.model.SortOrderDTO;
import io.kafbat.ui.model.TopicColumnsToSortDTO;
import io.kafbat.ui.model.TopicUpdateDTO;
import io.kafbat.ui.service.KafkaConnectService;
import io.kafbat.ui.service.TopicsService;
import io.kafbat.ui.service.analyze.TopicAnalysisService;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpSpecificationGeneratorTest {
  private static final SchemaGenerator SCHEMA_GENERATOR = schemaGenerator();
  private static final McpSpecificationGenerator MCP_SPECIFICATION_GENERATOR =
      new McpSpecificationGenerator(SCHEMA_GENERATOR, new ObjectMapper());

  private static SchemaGenerator schemaGenerator() {
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    return new SchemaGenerator(configBuilder.build());
  }

  @Test
  void testConvertController() {
    TopicsController topicsController = new TopicsController(
        mock(TopicsService.class), mock(TopicAnalysisService.class), mock(ClusterMapper.class),
        mock(ClustersProperties.class), mock(KafkaConnectService.class)
    );
    List<AsyncToolSpecification> specifications =
        MCP_SPECIFICATION_GENERATOR.convertTool(topicsController);

    assertThat(specifications).hasSize(16);
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
}
