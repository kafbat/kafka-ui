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
import io.kafbat.ui.service.acl.AclsService;
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
        mock(ClustersProperties.class), mock(KafkaConnectService.class), mock(AclsService.class)
    );
    List<AsyncToolSpecification> specifications =
        MCP_SPECIFICATION_GENERATOR.convertTool(topicsController);

    assertThat(specifications).hasSize(17);
    List<McpSchema.Tool> tools = List.of(
        McpSchema.Tool.builder()
            .name("recreateTopic").description("recreateTopic")
            .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName"), false, null, null))
            .build(),
        McpSchema.Tool.builder()
            .name("getTopicConfigs").description("getTopicConfigs")
            .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName"), false, null, null))
            .build(),
        McpSchema.Tool.builder()
            .name("cloneTopic").description("cloneTopic")
            .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string"),
                "newTopicName", Map.of("type", "string")
            ), List.of("clusterName", "topicName", "newTopicName"), false, null, null))
            .build(),
        McpSchema.Tool.builder()
            .name("getTopics").description("getTopics")
            .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "page", Map.of("type", "integer"),
                "perPage", Map.of("type", "integer"),
                "showInternal", Map.of("type", "boolean"),
                "search", Map.of("type", "string"),
                "orderBy", SCHEMA_GENERATOR.generateSchema(TopicColumnsToSortDTO.class),
                "sortOrder", SCHEMA_GENERATOR.generateSchema(SortOrderDTO.class),
                "fts", Map.of("type", "boolean")
            ), List.of("clusterName"), false, null, null))
            .build(),
        McpSchema.Tool.builder()
            .name("updateTopic").description("updateTopic")
            .inputSchema(new McpSchema.JsonSchema("object", Map.of(
                "clusterName", Map.of("type", "string"),
                "topicName", Map.of("type", "string"),
                "topicUpdate", SCHEMA_GENERATOR.generateSchema(TopicUpdateDTO.class)
            ), List.of("clusterName", "topicName", "topicUpdate"), false, null, null))
            .build()
    );
    assertThat(tools).allMatch(tool ->
        specifications.stream().anyMatch(s -> s.tool().equals(tool))
    );
  }
}
