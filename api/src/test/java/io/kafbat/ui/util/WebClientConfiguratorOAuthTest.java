package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.exception.OAuthTokenFetchException;
import java.io.IOException;
import java.time.Duration;
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
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token-123\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{\"result\":\"success\"}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("test-client-id");
      oauth.setClientSecret("test-client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      String result = webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getMethod()).isEqualTo("POST");
      assertThat(tokenRequest.getPath()).isEqualTo("/oauth/token");
      assertThat(tokenRequest.getHeader(HttpHeaders.CONTENT_TYPE))
          .contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
      assertThat(tokenRequest.getBody().readUtf8())
          .contains("grant_type=client_credentials")
          .contains("client_id=test-client-id")
          .contains("client_secret=test-client-secret");

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION))
          .isEqualTo("Bearer test-token-123");

      assertThat(result).contains("success");
    }

    @Test
    void shouldFetchNewTokenForEachRequestWhenCachingDisabled() throws InterruptedException {
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

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      RecordedRequest firstApiRequest = mockApiServer.takeRequest();
      RecordedRequest secondApiRequest = mockApiServer.takeRequest();

      assertThat(firstApiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-1");
      assertThat(secondApiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-2");
    }

    @Test
    void shouldPropagateErrorWhenTokenFetchFails() {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(401)
          .setBody("{\"error\":\"invalid_client\"}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("invalid-client");
      oauth.setClientSecret("invalid-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectErrorMatches(error ->
              error instanceof OAuthTokenFetchException
              && error.getMessage().contains("Failed to obtain OAuth access token")
              && error.getCause() instanceof WebClientResponseException.Unauthorized
          )
          .verify();
    }

    @Test
    void shouldHandleMissingAccessTokenInResponse() {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"token_type\":\"Bearer\",\"expires_in\":3600}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectErrorMatches(error ->
              error instanceof OAuthTokenFetchException
              && error.getMessage().contains("OAuth token response does not contain 'access_token' field")
          )
          .verify();
    }

    @Test
    void shouldNotConfigureOAuthWhenConfigIsNull() throws InterruptedException {
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(null)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenTokenUrlIsNull() throws InterruptedException {
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(null);
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

      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenClientIdIsNull() throws InterruptedException {
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId(null);
      oauth.setClientSecret("client-secret");

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldNotConfigureOAuthWhenClientSecretIsNull() throws InterruptedException {
      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret(null);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      assertThat(mockOAuthServer.getRequestCount()).isZero();

      RecordedRequest apiRequest = mockApiServer.takeRequest();
      assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void shouldIncludeScopeInTokenRequestWhenScopesConfigured() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setScopes(new String[] {"schema_registry", "read"});

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getBody().readUtf8())
          .contains("scope=schema_registry+read");
    }

    @Test
    void shouldNotIncludeScopeInTokenRequestWhenScopesNotConfigured() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

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

      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getBody().readUtf8()).doesNotContain("scope=");
    }

    @Test
    void shouldFetchTokenUsingConfiguredHttpClient() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"test-token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setBody("{}"));

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

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);
      RecordedRequest tokenRequest = mockOAuthServer.takeRequest();
      assertThat(tokenRequest.getPath()).isEqualTo("/oauth/token");
    }
  }

  @Nested
  class TokenCaching {

    @Test
    void shouldReuseTokenFromCacheForMultipleRequests() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"cached-token\",\"expires_in\":3600}"));

      for (int i = 0; i < 5; i++) {
        mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      }

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      for (int i = 0; i < 5; i++) {
        webClient.get()
            .uri(mockApiServer.url("/api/resource").toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();
      }

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      for (int i = 0; i < 5; i++) {
        RecordedRequest apiRequest = mockApiServer.takeRequest();
        assertThat(apiRequest.getHeader(HttpHeaders.AUTHORIZATION))
            .isEqualTo("Bearer cached-token");
      }
    }

    @Test
    void shouldNotCacheWhenCachingDisabled() throws InterruptedException {
      for (int i = 0; i < 3; i++) {
        mockOAuthServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"access_token\":\"token-" + i + "\",\"expires_in\":3600}"));
      }

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(false);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      for (int i = 0; i < 3; i++) {
        webClient.get()
            .uri(mockApiServer.url("/api/resource").toString())
            .retrieve()
            .bodyToMono(String.class)
            .block();
      }

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldHandleMissingExpiresIn() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-no-expiry\"}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      webClient.get().uri(mockApiServer.url("/api/1").toString()).retrieve().bodyToMono(String.class).block();
      webClient.get().uri(mockApiServer.url("/api/2").toString()).retrieve().bodyToMono(String.class).block();

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      RecordedRequest req1 = mockApiServer.takeRequest();
      RecordedRequest req2 = mockApiServer.takeRequest();
      assertThat(req1.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-no-expiry");
      assertThat(req2.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token-no-expiry");
    }

    @Test
    void shouldRespectRefreshBuffer() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-1\",\"expires_in\":30}"));
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-2\",\"expires_in\":30}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);
      oauth.setTokenRefreshBuffer(Duration.ofSeconds(90)); // Token effectively expires in 100-90=10s
      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();
      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();
      webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();
      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);
    }
  }

  @Nested
  class RetryOn401 {

    @Test
    void shouldRetryOnce401AndInvalidateCache() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"expired-token\",\"expires_in\":3600}"));

      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"fresh-token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(401));
      mockApiServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}"));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setTokenCacheEnabled(true);
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      String result = webClient.get()
          .uri(mockApiServer.url("/api/resource").toString())
          .retrieve()
          .bodyToMono(String.class)
          .block();

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      assertThat(mockApiServer.getRequestCount()).isEqualTo(2);

      assertThat(result).contains("success");

      RecordedRequest firstApiReq = mockApiServer.takeRequest();
      RecordedRequest secondApiReq = mockApiServer.takeRequest();
      assertThat(firstApiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer expired-token");
      assertThat(secondApiReq.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer fresh-token");
    }

    @Test
    void shouldNotRetryAfterMaxRetries() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-1\",\"expires_in\":3600}"));

      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token-2\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(401));
      mockApiServer.enqueue(new MockResponse().setResponseCode(401));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Unauthorized.class)
          .verify();

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(2);

      assertThat(mockApiServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryWhenMaxRetriesIsZero() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(401));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(0);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Unauthorized.class)
          .verify();

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      assertThat(mockApiServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldNotRetryOn403Forbidden() throws InterruptedException {
      mockOAuthServer.enqueue(new MockResponse()
          .setResponseCode(200)
          .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .setBody("{\"access_token\":\"token\",\"expires_in\":3600}"));

      mockApiServer.enqueue(new MockResponse().setResponseCode(403));

      ClustersProperties.OauthConfig oauth = new ClustersProperties.OauthConfig();
      oauth.setTokenUrl(mockOAuthServer.url("/oauth/token").toString());
      oauth.setClientId("client-id");
      oauth.setClientSecret("client-secret");
      oauth.setMaxRetries(1);

      WebClient webClient = new WebClientConfigurator()
          .configureOAuth(oauth)
          .build();

      StepVerifier.create(
          webClient.get()
              .uri(mockApiServer.url("/api/resource").toString())
              .retrieve()
              .bodyToMono(String.class)
      )
          .expectError(WebClientResponseException.Forbidden.class)
          .verify();

      assertThat(mockOAuthServer.getRequestCount()).isEqualTo(1);

      assertThat(mockApiServer.getRequestCount()).isEqualTo(1);
    }
  }
}
