package io.kafbat.ui.util;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import reactor.core.publisher.Mono;

/**
 * Thread-safe cache for OAuth access tokens using Caffeine's AsyncCache.
 * Concurrent requests on a cache miss share a single in-flight fetch rather than
 * each independently calling the OAuth server.
 */
@Slf4j
public class OAuthTokenCache {

  private static final String CACHE_KEY = "oauth_token";
  private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;

  private record TokenWithExpiry(String accessToken, long expiresInSeconds) {}

  private final AsyncCache<String, TokenWithExpiry> cache;
  private final long refreshBufferSeconds;

  public OAuthTokenCache(Duration refreshBuffer) {
    this.refreshBufferSeconds = refreshBuffer.toSeconds();

    this.cache = Caffeine.newBuilder()
        .maximumSize(1)
        .expireAfter(new Expiry<String, TokenWithExpiry>() {
          @Override
          public long expireAfterCreate(String key, TokenWithExpiry value, long currentTime) {
            log.debug("OAuth token cached, effective TTL: {}s", value.expiresInSeconds());
            return TimeUnit.SECONDS.toNanos(value.expiresInSeconds());
          }

          @Override
          public long expireAfterUpdate(String key, TokenWithExpiry value,
                                        long currentTime, @NonNegative long currentDuration) {
            return expireAfterCreate(key, value, currentTime);
          }

          @Override
          public long expireAfterRead(String key, TokenWithExpiry value,
                                      long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
          }
        })
        .buildAsync();

    log.debug("Created OAuth token cache with {}s refresh buffer", refreshBufferSeconds);
  }

  public Mono<String> getToken(Supplier<Mono<OAuthTokenResponse>> fetcher) {
    return Mono.fromFuture(
        cache.get(CACHE_KEY, (key, executor) ->
            fetcher.get()
                .map(response -> {
                  long expiresIn = response.hasExpiresIn()
                      ? response.getExpiresIn() : DEFAULT_EXPIRES_IN_SECONDS;
                  if (!response.hasExpiresIn()) {
                    log.warn("OAuth server did not provide expires_in, using default {}s",
                        DEFAULT_EXPIRES_IN_SECONDS);
                  }
                  long effectiveExpiresIn = Math.max(0, expiresIn - refreshBufferSeconds);
                  return new TokenWithExpiry(response.getAccessToken(), effectiveExpiresIn);
                })
                .toFuture()
        )
    ).map(TokenWithExpiry::accessToken);
  }

  public void invalidate() {
    cache.synchronous().invalidateAll();
    log.debug("OAuth token cache invalidated");
  }
}
