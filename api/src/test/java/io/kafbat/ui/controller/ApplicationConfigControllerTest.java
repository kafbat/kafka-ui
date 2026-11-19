package io.kafbat.ui.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.ApplicationConfigDTO;
import io.kafbat.ui.model.ApplicationConfigPropertiesDTO;
import io.kafbat.ui.model.UploadedFileInfoDTO;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;

class ApplicationConfigControllerTest extends AbstractIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testUpload() throws IOException {
    var fileToUpload = new ClassPathResource("/fileForUploadTest.txt", this.getClass());

    UploadedFileInfoDTO result = webTestClient
        .post()
        .uri("/api/config/relatedfiles")
        .bodyValue(generateBody(fileToUpload))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(UploadedFileInfoDTO.class)
        .returnResult()
        .getResponseBody();

    assertThat(result).isNotNull();
    assertThat(result.getLocation()).isNotNull();
    assertThat(Path.of(result.getLocation()))
        .hasSameBinaryContentAs(fileToUpload.getFile().toPath());
  }

  private MultiValueMap<String, HttpEntity<?>> generateBody(ClassPathResource resource) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", resource);
    return builder.build();
  }

  @Nested
  @TestPropertySource(properties = "dynamic.config.enabled=false")
  class DynamicConfigDisabledTest {

    @Test
    void validateConfigReturnsErrorWhenDynamicConfigDisabled(@Autowired WebTestClient client) {
      client
          .put()
          .uri("/api/config/validated")
          .bodyValue(new ApplicationConfigDTO().properties(new ApplicationConfigPropertiesDTO()))
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Dynamic config change is not allowed. "
              + "Set dynamic.config.enabled property to 'true' to enabled it.");
    }
  }

}
