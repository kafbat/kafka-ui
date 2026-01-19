package io.kafbat.ui.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Tests for {@link WebClientConfigurator}.
 */
public class WebClientConfiguratorTest {

  @Test
  void testCreateDefaultObjectMapper() {
    ObjectMapper mapper = WebClientConfigurator.createDefaultObjectMapper();
    assertNotNull(mapper, "Default ObjectMapper should not be null");
  }

  @Test
  void testConfigureObjectMapperWithMediaTypes() {
    // Test that configureObjectMapperWithMediaTypes accepts custom media types
    // and builds a WebClient without errors
    MediaType customMediaType = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json");
    
    WebClient webClient = new WebClientConfigurator()
        .configureObjectMapperWithMediaTypes(
            WebClientConfigurator.createDefaultObjectMapper(),
            customMediaType
        )
        .build();
    
    assertNotNull(webClient, "WebClient should not be null");
  }

  @Test
  void testConfigureObjectMapperWithMultipleMediaTypes() {
    // Test that configureObjectMapperWithMediaTypes accepts multiple custom media types
    MediaType customMediaType1 = MediaType.parseMediaType("application/vnd.schemaregistry.v1+json");
    MediaType customMediaType2 = MediaType.parseMediaType("application/vnd.schemaregistry+json");
    
    WebClient webClient = new WebClientConfigurator()
        .configureObjectMapperWithMediaTypes(
            WebClientConfigurator.createDefaultObjectMapper(),
            customMediaType1,
            customMediaType2
        )
        .build();
    
    assertNotNull(webClient, "WebClient should not be null");
  }

  @Test
  void testConfigureObjectMapperWithNoAdditionalMediaTypes() {
    // Test that configureObjectMapperWithMediaTypes works with no additional media types
    WebClient webClient = new WebClientConfigurator()
        .configureObjectMapperWithMediaTypes(
            WebClientConfigurator.createDefaultObjectMapper()
        )
        .build();
    
    assertNotNull(webClient, "WebClient should not be null");
  }
}
