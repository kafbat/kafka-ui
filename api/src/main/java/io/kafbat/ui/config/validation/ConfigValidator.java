package io.kafbat.ui.config.validation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigValidator {

    private final ConfigValidationHealthIndicator healthIndicator;

    @PostConstruct
    public void init() {
        log.info("Configuration validator initialized");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup(ApplicationReadyEvent event) {
        healthIndicator.validateConfiguration()
            .subscribe(valid -> {
                if (!valid) {
                    log.error("Application started with invalid configuration. " +
                             "Check health endpoint for details. Application may not function correctly.");
                } else {
                    log.info("Application started with valid configuration");
                }
            });
    }
}
