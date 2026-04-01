package io.kafbat.ui.client;

import io.kafbat.ui.config.ConfluentAuthConfig;
import io.kafbat.ui.config.HttpFeignConfig;
import io.kafbat.ui.model.sainsburys.SchemaMetadataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="confluent-api-client", url="${kit.external.services.confluent-api.base-url}", configuration = {
    HttpFeignConfig.class, ConfluentAuthConfig.class})
public interface ConfluentApiClient {

  @GetMapping(path = "${kit.external.services.confluent-api.operations.retrieve.tagdefs}")
  ResponseEntity<SchemaMetadataResponse> retrieveTagDefinitions();

  @GetMapping(path = "${kit.external.services.confluent-api.operations.retrieve.topic}")
  ResponseEntity<SchemaMetadataResponse> retrieveTopicMetadata(@RequestParam("tag") String tag);

  @GetMapping(path = "${kit.external.services.confluent-api.operations.retrieve.topic.fields}")
  ResponseEntity<SchemaMetadataResponse> retrieveTopicFieldsMetadata();

  @GetMapping(path = "${kit.external.services.confluent-api.operations.retrieve.schema}")
  ResponseEntity<SchemaMetadataResponse> retrieveSchemaMetadata();
}
