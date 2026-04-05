package io.kafbat.ui.util;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.OAuthTokenFetchException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Handles OAuth 2.0 client credentials token acquisition and caching for a single OAuth config.
 * Reuses a single WebClient instance across all token requests.
 */
@Slf4j
public class OAuthTokenProvider {

  private final ClustersProperties.OauthConfig oauth;
  private final WebClient tokenClient;
  private final OAuthTokenCache tokenCache;

  public OAuthTokenProvider(ClustersProperties.OauthConfig oauth, HttpClient httpClient) {
    this.oauth = oauth;
    this.tokenClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
    this.tokenCache = Boolean.TRUE.equals(oauth.getTokenCacheEnabled())
        ? new OAuthTokenCache(oauth.getTokenRefreshBuffer())
        : null;
  }

  public Mono<String> getAccessToken() {
    if (tokenCache != null) {
      return tokenCache.getToken(this::fetchTokenResponse);
    }
    return fetchTokenResponse().map(OAuthTokenResponse::getAccessToken);
  }

  public void invalidateCache() {
    if (tokenCache != null) {
      tokenCache.invalidate();
    }
  }

  private Mono<OAuthTokenResponse> fetchTokenResponse() {
    log.debug("Fetching OAuth access token from {}", oauth.getTokenUrl());

    String requestBody = "grant_type=client_credentials&client_id="
        + URLEncoder.encode(oauth.getClientId(), StandardCharsets.UTF_8)
        + "&client_secret="
        + URLEncoder.encode(oauth.getClientSecret(), StandardCharsets.UTF_8);

    if (oauth.getScopes() != null && oauth.getScopes().length > 0) {
      String scope = String.join(" ", oauth.getScopes());
      requestBody += "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
    }

    return tokenClient
        .post()
        .uri(oauth.getTokenUrl())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(OAuthTokenResponse.class)
        .doOnNext(response -> log.debug("OAuth token response received: accessToken present: {}, expiresIn: {}s",
            response.hasAccessToken(), response.getExpiresIn()))
        .flatMap(response -> {
          if (!response.hasAccessToken()) {
            log.error("OAuth token response does not contain 'access_token' field");
            return Mono.error(
                new OAuthTokenFetchException("OAuth token response does not contain 'access_token' field"));
          }
          log.debug("OAuth access token obtained, expires in {}s",
              response.getExpiresIn() != null ? response.getExpiresIn() : "unknown");
          return Mono.just(response);
        })
        .onErrorResume(OAuthTokenFetchException.class, Mono::error)
        .onErrorResume(error -> {
          log.error("Failed to obtain OAuth access token from {}: {}", oauth.getTokenUrl(),
              error.getMessage(), error);
          return Mono.error(
              new OAuthTokenFetchException("Failed to obtain OAuth access token from " + oauth.getTokenUrl()
                  + ": " + error.getMessage(), error));
        });
  }
}
