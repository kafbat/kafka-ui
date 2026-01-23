package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

class WebClientConfiguratorOAuthTest {

  private MockWebServer mockOAuthServer;
  private MockWebServer mockApiServer;

  @BeforeEach
  void setUp() throws IOException {
    mockOAuthServer = new MockWebServer();
    mockOAuthServer.start();

    mockApiServer = new MockWebServer();
    mockApiServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockOAuthServer.shutdown();
    mockApiServer.shutdown();
  }

  @Nested
  class OAuthConfiguration {

    @Test
    void shouldFetchTokenAndAddBearerAuthToRequests() throws InterruptedException {
      // Given: OAuth server returns a valid token
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token-123\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

      // And: API server expects Bearer token
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{\"result\":\"success\"}"));

      // When: WebClient is configured with OAuth
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("test-client-id");
      oauth.setClientSecret("test-client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make a request to the API
      String result = webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: Token request should be made with correct credentials
      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getMethod()).isEqualTo("POST");
      assertThat(tokenRequest.getPath()).isEqualTo("/oauth/token");
      assertThat(tokenRequest.getHeader(HttpHeaders.CONTENT_TYPE))
          .contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
      assertThat(tokenRequest.getBody().readUtf8())
          .contains("grant_type=client_credentials")
          .contains("client_id=test-client-id")
          .contains("client_secret=test-client-secret");

      // And: API request should include Bearer token
      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION))
          .isEqualTo("Bearer test-token-123");

      // And: Response should be successful
      assertThat(result).contains("success");
    }

    @Test
    void shouldFetchNewTokenForEachRequestWhenCachingDisabled() throws InterruptedException {
      // Given: OAuth server returns different tokens for each request
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-1\",\"expires_in\":3600}"));
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-2\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      // When: Multiple requests are made with caching disabled
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(false); // Explicitly disable caching

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get().uri(mockApiServer.url("/api/1").toString()).retrieve().bodyToMono(String.class).block();
      webClient.get().uri(mockApiServer.url("/api/2").toString()).retrieve().bodyToMono(String.class).block();

      // Then: Token should be fetched for each request
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      // And: Different tokens should be used
      RecordedRequest firstApiRequest = mockApiServer.takeRequest();
      RecordedRequest secondApiRequest = mockApiServer.takeRequest();

      assertThat(firstApiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-1");
      assertThat(secondApiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-2");
    }

    @Test
    void shouldPropagateErrorWhenTokenFetchFails() {
      // Given: OAuth server returns error
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(401)
          .setBody("{\"error\":\"invalid_client\"}"));

      // When: Request is made with OAuth configured
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("invalid-client");
      oauth.setClientSecret("invalid-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // Then: Should fail with appropriate error
      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectErrorMatches(error ->
              error instanceof RuntimeException
              && error.getMessage().contains("Failed to obtain OAuth access token")
              && error.getCause() instanceof WebClientResponseException.Unauthorized
          )
          .verify();
    }

    @Test
    void shouldHandleMissingAccessTokenInResponse() {
      // Given: OAuth server returns response without access_token field
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"token_type\":\"Bearer\",\"expires_in\":3600}"));

      // When: Request is made
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // Then: Should fail with clear error message
      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectErrorMatches(error ->
              error instanceof RuntimeException
              && error.getMessage().contains("OAuth token response does not contain 'access_token' field")
          )
          .verify();
    }

    @Test
    void shouldNotConfigureOAuthWhenConfigIsNull() throws InterruptedException {
      // Given: OAuth config is null
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      // When: WebClient is configured without OAuth
      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(null)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: No token request should be made
      assertThat(mockOAuthServer.getRequestCount()).isZero();

      // And: API request should not include Authorization header
      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenTokenUrlIsNull() throws InterruptedException {
      // Given: OAuth config with null tokenUrl
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(null);
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");

      // When: WebClient is configured
      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: No OAuth filter should be applied
      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenClientIdIsNull() throws InterruptedException {
      // Given: OAuth config with null clientId
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId(null);
      oauth.setClientSecret("client-secret");

      // When: WebClient is configured
      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: No OAuth filter should be applied
      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenClientSecretIsNull() throws InterruptedException {
      // Given: OAuth config with null clientSecret
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret(null);

      // When: WebClient is configured
      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: No OAuth filter should be applied
      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldUseProxySettingsForTokenRequest() throws InterruptedException {
      // Given: OAuth server returns token
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      // When: WebClient is configured with OAuth
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: Token request should be made (verifying httpClient configuration is used)
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);
      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getPath()).isEqualTo("/oauth/token");
    }
  }

  @Nested
  class TokenCaching {

    @Test
    void shouldReuseTokenFromCacheForMultipleRequests() throws InterruptedException {
      // Given: OAuth server returns token with expiration
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"cached-token\",\"expires_in\":3600}"));

      // And: API server handles multiple requests
      for (int i = 0; i < 5; i++) {
        mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      }

      // When: WebClient with caching enabled
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make multiple requests
      for (int i = 0; i < 5; i++) {
        webClient.get()
            .uri(mockApiServer.url("/api/resource").toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();
      }

      // Then: Only one token request should be made
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      // And: All API requests should use the same cached token
      for (int i = 0; i < 5; i++) {
        RecordedRequest apiRequest = mockApiServer.takeRequest();
        assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION))
            .isEqualTo("Bearer cached-token");
      }
    }

    @Test
    void shouldNotCacheWhenCachingDisabled() throws InterruptedException {
      // Given: OAuth server returns tokens
      for (int i = 0; i < 3; i++) {
        mockOAuthServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"access_token\":\"token-" + i + "\",\"expires_in\":3600}"));
      }

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      // When: WebClient with caching disabled
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(false);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make multiple requests
      for (int i = 0; i < 3; i++) {
        webClient.get()
            .uri(mockApiServer.url("/api/resource").toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();
      }

      // Then: Each request should fetch a new token
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldHandleMissingExpiresIn() throws InterruptedException {
      // Given: OAuth server returns token without expires_in
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-no-expiry\"}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      // When: WebClient with caching enabled
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make multiple requests
      webClient.get().uri(mockApiServer.url("/api/1").toString()).retrieve().bodyToMono(String.class).block();
      webClient.get().uri(mockApiServer.url("/api/2").toString()).retrieve().bodyToMono(String.class).block();

      // Then: Token should still be cached (with default 3600s expiration)
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      // And: Both requests should use same token
      RecordedRequest req1 = mockApiServer.takeRequest();
      RecordedRequest req2 = mockApiServer.takeRequest();
      assertThat(req1.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-no-expiry");
      assertThat(req2.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-no-expiry");
    }

    @Test
    void shouldRespectRefreshBuffer() throws InterruptedException {
      // Given: OAuth server returns token expiring in 100s
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"short-lived-token\",\"expires_in\":100}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      // When: WebClient with large refresh buffer (90s)
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);
      oauth.setTokenRefreshBufferSeconds(90); // Token effectively expires in 100-90=10s

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make a request
      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: Token should be fetched
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);
    }
  }

  @Nested
  class RetryOn401 {

    @Test
    void shouldRetryOnce401AndInvalidateCache() throws InterruptedException {
      // Given: First token gets rejected, second succeeds
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"expired-token\",\"expires_in\":3600}"));

      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"fresh-token\",\"expires_in\":3600}"));

      // And: API server rejects first token, accepts second
      mockApiServer.enqueue(new MockResponse().setResponseCode(401));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}"));

      // When: WebClient configured with retry
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // And: Make request
      String result = webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      // Then: Should have fetched 2 tokens (original + retry)
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      // And: Should have made 2 API requests
      assertThat(mockApiServer.getRequestCount()).isEqualTo(2);

      // And: Second request should succeed
      assertThat(result).contains("success");

      // Verify tokens used
      RecordedRequest firstApiReq = mockApiServer.takeRequest();
      RecordedRequest secondApiReq = mockApiServer.takeRequest();
      assertThat(firstApiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer expired-token");
      assertThat(secondApiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer fresh-token");
    }

    @Test
    void shouldNotRetryAfterMaxRetries() throws InterruptedException {
      // Given: OAuth server returns tokens
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-1\",\"expires_in\":3600}"));

      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-2\",\"expires_in\":3600}"));

      // And: API server always rejects with 401
      mockApiServer.enqueue(new MockResponse().setResponseCode(401));
      mockApiServer.enqueue(new MockResponse().setResponseCode(401));

      // When: WebClient configured with maxRetries=1
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // Then: Should fail after 1 retry
      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Unauthorized.class)
          .verify();

      // And: Should have fetched 2 tokens (original + 1 retry)
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      // And: Should have made 2 API requests (original + 1 retry)
      assertThat(mockApiServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryWhenMaxRetriesIsZero() throws InterruptedException {
      // Given: OAuth server returns token
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token\",\"expires_in\":3600}"));

      // And: API server returns 401
      mockApiServer.enqueue(new MockResponse().setResponseCode(401));

      // When: WebClient configured with maxRetries=0
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(0);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // Then: Should fail immediately without retry
      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Unauthorized.class)
          .verify();

      // And: Should have fetched only 1 token
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      // And: Should have made only 1 API request
      assertThat(mockApiServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldNotRetryOn403Forbidden() throws InterruptedException {
      // Given: OAuth server returns token
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token\",\"expires_in\":3600}"));

      // And: API server returns 403 Forbidden
      mockApiServer.enqueue(new MockResponse().setResponseCode(403));

      // When: WebClient configured with retry
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      // Then: Should fail without retry (403 is not retryable)
      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Forbidden.class)
          .verify();

      // And: Should have fetched only 1 token
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      // And: Should have made only 1 API request
      assertThat(mockApiServer.getRequestCount()).isEqualTo(1);
    }
  }
}
