package io.kafbat.ui.controller;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.model.UploadedFileInfoDTO;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.MultipartBodyBuilder;
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

}
