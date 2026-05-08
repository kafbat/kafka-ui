package io.kafbat.ui.client;

import io.kafbat.ui.config.sainsburys.HttpFeignConfig;
import io.kafbat.ui.model.sainsburys.confluent.SchemaMetadataResponse;
import io.kafbat.ui.model.sainsburys.confluent.SubjectMetadataResponse;
import io.kafbat.ui.model.sainsburys.confluent.TagDefinitionClassificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;
import java.util.List;

@FeignClient(name="confluent-api-client", url="${sainsburys.external.services.confluent-api.base-url}", configuration = {
    HttpFeignConfig.class})
public interface ConfluentApiClient {

  @GetMapping(path = "${sainsburys.external.services.confluent-api.operations.retrieve.tagdefs}")
  ResponseEntity<List<TagDefinitionClassificationResponse>> retrieveTagDefinitions(URI baseUrl,
                                                                                   @RequestHeader("Authorization") String authorization);

  @GetMapping(path = "${sainsburys.external.services.confluent-api.operations.retrieve.topic}")
  ResponseEntity<SchemaMetadataResponse> retrieveTopicMetadata(URI baseUrl,
                                                               @RequestHeader("Authorization") String authorization,
                                                               @RequestParam("tag") String tag);

  @GetMapping(path = "${sainsburys.external.services.confluent-api.operations.retrieve.topic.fields}")
  ResponseEntity<SchemaMetadataResponse> retrieveTopicFieldsMetadata(URI baseUrl,
                                                                     @RequestHeader("Authorization") String authorization,
                                                                     @RequestParam("tag") String tag);

  @GetMapping(path = "${sainsburys.external.services.confluent-api.operations.retrieve.schema}")
  ResponseEntity<SchemaMetadataResponse> retrieveSchemaMetadata(URI baseUrl,
                                                                @RequestHeader("Authorization") String authorization);

  @GetMapping(path = "${sainsburys.external.services.confluent-api.operations.retrieve.subject}")
  ResponseEntity<SubjectMetadataResponse> retrieveSubjectMetadata(URI baseUrl,
                                                                  @RequestHeader("Authorization") String authorization,
                                                                  @PathVariable("topic") String subject);
}
