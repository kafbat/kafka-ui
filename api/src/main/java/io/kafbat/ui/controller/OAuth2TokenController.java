package io.kafbat.ui.controller;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.exception.NotFoundException;
import io.kafbat.ui.exception.ValidationException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class OAuth2TokenController {

  private final OAuthProperties oAuthProperties;
  private final WebClient oauthWebClient;

  public OAuth2TokenController(
      OAuthProperties oAuthProperties,
      @Qualifier("oauthWebClient") WebClient oauthWebClient) {
    this.oAuthProperties = oAuthProperties;
    this.oauthWebClient = oauthWebClient;
  }

  @PostMapping(value = "/api/token", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Map<String, String>>> getToken(@RequestBody TokenRequest request) {

    OAuthProperties.OAuth2Provider provider = findProvider(request.getClientId());
    if (provider == null) {
      return Mono.error(new NotFoundException(
          "No OAuth2 provider found for clientId: " + request.getClientId()));
    }

    if (!provider.isApiTokenEnabled()) {
      return Mono.error(new ValidationException(
          "API token generation is not enabled for this provider"));
    }

    // Null-safe comparison to prevent NPE when clientSecret is absent
    if (!Objects.equals(provider.getClientSecret(), request.getClientSecret())) {
      return Mono.error(new ValidationException("Invalid client credentials"));
    }

    String tokenUrl = provider.getTokenUri();
    if (tokenUrl == null || tokenUrl.isBlank()) {
      return Mono.error(new ValidationException(
          "Token URL is not configured for this provider"));
    }

    String requestBody = "grant_type=client_credentials"
        + "&client_id=" + URLEncoder.encode(request.getClientId(), StandardCharsets.UTF_8)
        + "&client_secret=" + URLEncoder.encode(request.getClientSecret(), StandardCharsets.UTF_8);

    if (request.getScope() != null && !request.getScope().isBlank()) {
      requestBody += "&scope=" + URLEncoder.encode(request.getScope(), StandardCharsets.UTF_8);
    }

    return oauthWebClient
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(Map.class)
        .map(response -> {
          @SuppressWarnings("unchecked")
          Map<String, String> tokenResponse = (Map<String, String>) response;
          return ResponseEntity.ok(tokenResponse);
        })
        .doOnError(e -> log.error("Failed to obtain OAuth token for clientId {}: {}",
            request.getClientId(), e.getMessage(), e))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(401).body(
            Map.of("error", "authentication_failed", "message", "Authentication failed"))));
  }

  private OAuthProperties.OAuth2Provider findProvider(String clientId) {
    return oAuthProperties.getClient().values().stream()
        .filter(p -> p.getClientId().equals(clientId))
        .findFirst()
        .orElse(null);
  }

  @Data
  public static class TokenRequest {
    private String clientId;
    private String clientSecret;
    private String scope;
  }
}