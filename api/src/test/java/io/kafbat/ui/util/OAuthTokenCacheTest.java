package io.kafbat.ui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OAuthTokenCacheTest {

  private static OAuthTokenResponse response(String token, long expiresIn) {
    OAuthTokenResponse r = new OAuthTokenResponse();
    r.setAccessToken(token);
    r.setExpiresIn(expiresIn);
    return r;
  }

  private static OAuthTokenResponse responseNoExpiry(String token) {
    OAuthTokenResponse r = new OAuthTokenResponse();
    r.setAccessToken(token);
    return r;
  }

  @Test
  void shouldFetchTokenOnCacheMiss() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));

    StepVerifier.create(cache.getToken(() -> Mono.just(response("token-1", 3600))))
        .expectNext("token-1")
        .verifyComplete();
  }

  @Test
  void shouldReturnCachedTokenWithoutCallingFetcherAgain() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));
    AtomicInteger fetchCount = new AtomicInteger();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("cached-token", 3600));
    }).block();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("new-token", 3600));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(1);
  }

  @Test
  void shouldReturnCachedTokenValue() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));

    cache.getToken(() -> Mono.just(response("token-1", 3600))).block();

    String second = cache.getToken(() -> Mono.just(response("token-2", 3600))).block();
    assertThat(second).isEqualTo("token-1");
  }

  @Test
  void shouldFetchNewTokenAfterExpiry() {
    // buffer > expiresIn → effectiveExpiresIn = max(0, 1 - 5) = 0 → entry expires immediately
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(5));
    AtomicInteger fetchCount = new AtomicInteger();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-1", 1));
    }).block();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-2", 3600));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(2);
  }

  @Test
  void shouldApplyRefreshBuffer() throws InterruptedException {
    // Buffer of 90s on a token that expires in 30s → effective TTL = 0 → treated as expired immediately
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(90));
    AtomicInteger fetchCount = new AtomicInteger();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-1", 30));
    }).block();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-2", 3600));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(2);
  }

  @Test
  void shouldFetchNewTokenAfterInvalidation() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));
    AtomicInteger fetchCount = new AtomicInteger();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-1", 3600));
    }).block();

    cache.invalidate();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-2", 3600));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(2);
  }

  @Test
  void shouldUseDefaultExpiryWhenServerOmitsExpiresIn() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));
    AtomicInteger fetchCount = new AtomicInteger();

    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(responseNoExpiry("token-1"));
    }).block();

    // Second call should still use cache (default 3600s TTL applied)
    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(responseNoExpiry("token-2"));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(1);
  }

  @Test
  void shouldPropagateFetcherError() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));

    StepVerifier.create(
        cache.getToken(() -> Mono.error(new RuntimeException("OAuth server unavailable")))
    )
        .expectErrorMessage("OAuth server unavailable")
        .verify();
  }

  @Test
  void shouldRetryAfterFetcherError() {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));
    AtomicInteger fetchCount = new AtomicInteger();

    // First call fails
    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.error(new RuntimeException("transient error"));
    }).onErrorResume(e -> Mono.empty()).block();

    // Second call should retry (failed futures are not cached)
    cache.getToken(() -> {
      fetchCount.incrementAndGet();
      return Mono.just(response("token-1", 3600));
    }).block();

    assertThat(fetchCount.get()).isEqualTo(2);
  }

  @Test
  void shouldCoordinateConcurrentCacheMisses() throws InterruptedException {
    OAuthTokenCache cache = new OAuthTokenCache(Duration.ofSeconds(0));
    AtomicInteger fetchCount = new AtomicInteger();
    int threadCount = 20;

    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new Thread(() ->
          cache.getToken(() -> {
            fetchCount.incrementAndGet();
            return Mono.just(response("shared-token", 3600));
          }).block()
      );
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    assertThat(fetchCount.get()).isEqualTo(1);
  }
}
