package io.kafbat.ui.client;

import io.kafbat.ui.config.sainsburys.HttpFeignConfig;
import io.kafbat.ui.config.sainsburys.ServiceNowAuthConfig;
import io.kafbat.ui.model.sainsburys.servicenow.ServiceNowCreate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="servicenow-client", url="${kit.external.services.service-now.base-url}", configuration = {
    HttpFeignConfig.class, ServiceNowAuthConfig.class})
public interface ServiceNowClient {
  @PostMapping(path = "${kit.external.services.service-now.operations.create}")
  ResponseEntity<Object> createAuditTicket(@RequestBody ServiceNowCreate payload);
}
