package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OAuthTokenCacheTest {

  @Test
  void shouldReturnEmptyWhenNoTokenCached() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    Optional<String> token = cache.getValidToken();

    assertThat(token).isEmpty();
  }

  @Test
  void shouldReturnCachedTokenWhenValid() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken("test-token-123", 3600);
    Optional<String> token = cache.getValidToken();

    assertThat(token).isPresent();
    assertThat(token.get()).isEqualTo("test-token-123");
  }

  @Test
  void shouldReturnEmptyWhenTokenExpired() throws InterruptedException {
    OAuthTokenCache cache = new OAuthTokenCache(0); // No buffer

    // Store token that expires in 1 second
    cache.storeToken("expiring-token", 1);

    // Wait for expiration
    Thread.sleep(1100);

    Optional<String> token = cache.getValidToken();

    assertThat(token).isEmpty();
  }

  @Test
  void shouldApplyRefreshBuffer() {
    OAuthTokenCache cache = new OAuthTokenCache(300); // 5 minute buffer

    // Store token that expires in 400 seconds
    cache.storeToken("test-token", 400);

    // Token should be cached for 400 - 300 = 100 seconds
    Optional<Instant> expiration = cache.getExpirationTime();
    assertThat(expiration).isPresent();

    // Effective expiration should be roughly 100 seconds from now
    long effectiveSeconds = expiration.get().getEpochSecond() - Instant.now().getEpochSecond();
    assertThat(effectiveSeconds).isBetween(95L, 105L); // Allow 5s variance
  }

  @Test
  void shouldInvalidateCache() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken("test-token", 3600);
    assertThat(cache.getValidToken()).isPresent();

    cache.invalidate();

    assertThat(cache.getValidToken()).isEmpty();
    assertThat(cache.hasToken()).isFalse();
  }

  @Test
  void shouldHandleInvalidationWhenEmpty() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    // Should not throw exception
    cache.invalidate();

    assertThat(cache.hasToken()).isFalse();
  }

  @Test
  void shouldIgnoreNullToken() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken(null, 3600);

    assertThat(cache.getValidToken()).isEmpty();
    assertThat(cache.hasToken()).isFalse();
  }

  @Test
  void shouldIgnoreEmptyToken() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken("", 3600);

    assertThat(cache.getValidToken()).isEmpty();
    assertThat(cache.hasToken()).isFalse();
  }

  @Test
  void shouldIgnoreInvalidExpiration() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken("test-token", 0);
    assertThat(cache.hasToken()).isFalse();

    cache.storeToken("test-token", -100);
    assertThat(cache.hasToken()).isFalse();
  }

  @Test
  void shouldOverwritePreviousToken() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    cache.storeToken("token-1", 3600);
    assertThat(cache.getValidToken()).contains("token-1");

    cache.storeToken("token-2", 3600);
    assertThat(cache.getValidToken()).contains("token-2");
  }

  @Test
  void shouldBeThreadSafe() throws InterruptedException {
    OAuthTokenCache cache = new OAuthTokenCache(60);
    int threadCount = 10;
    int operationsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<Exception> exceptions = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(() -> {
        try {
          for (int j = 0; j < operationsPerThread; j++) {
            cache.storeToken("token-" + threadId + "-" + j, 3600);
            cache.getValidToken();
            if (j % 10 == 0) {
              cache.invalidate();
            }
          }
        } catch (Exception e) {
          exceptions.add(e);
        } finally {
          latch.countDown();
        }
      });
    }

    boolean completed = latch.await(10, TimeUnit.SECONDS);

    executor.shutdown();
    assertThat(completed).isTrue();
    assertThat(exceptions).isEmpty();
  }

  @Test
  void shouldHandleConcurrentReads() throws InterruptedException {
    OAuthTokenCache cache = new OAuthTokenCache(60);
    cache.storeToken("shared-token", 3600);

    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    List<String> results = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          Optional<String> token = cache.getValidToken();
          token.ifPresent(results::add);
        } finally {
          latch.countDown();
        }
      });
    }

    boolean completed = latch.await(5, TimeUnit.SECONDS);

    executor.shutdown();
    assertThat(completed).isTrue();
    assertThat(results).hasSize(threadCount);
    assertThat(results).allMatch(token -> token.equals("shared-token"));
  }

  @Test
  void shouldHandleZeroRefreshBuffer() {
    OAuthTokenCache cache = new OAuthTokenCache(0);

    cache.storeToken("test-token", 100);

    Optional<Instant> expiration = cache.getExpirationTime();
    assertThat(expiration).isPresent();

    // With zero buffer, effective expiration should be ~100 seconds from now
    long effectiveSeconds = expiration.get().getEpochSecond() - Instant.now().getEpochSecond();
    assertThat(effectiveSeconds).isBetween(95L, 105L);
  }

  @Test
  void shouldHandleLargeRefreshBuffer() {
    OAuthTokenCache cache = new OAuthTokenCache(5000); // Buffer larger than expiration

    cache.storeToken("test-token", 3600);

    // Token should be treated as immediately expired since buffer > expires_in
    // Effective expiration = max(0, 3600 - 5000) = 0
    Optional<String> token = cache.getValidToken();
    assertThat(token).isEmpty();
  }

  @Test
  void shouldProvideExpirationInfo() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    assertThat(cache.getExpirationTime()).isEmpty();

    cache.storeToken("test-token", 3600);

    Optional<Instant> expiration = cache.getExpirationTime();
    assertThat(expiration).isPresent();
    assertThat(expiration.get()).isAfter(Instant.now());
  }

  @Test
  void shouldIndicateTokenPresence() {
    OAuthTokenCache cache = new OAuthTokenCache(60);

    assertThat(cache.hasToken()).isFalse();

    cache.storeToken("test-token", 3600);
    assertThat(cache.hasToken()).isTrue();

    cache.invalidate();
    assertThat(cache.hasToken()).isFalse();
  }
}
