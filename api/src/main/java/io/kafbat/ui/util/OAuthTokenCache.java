package io.kafbat.ui.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * Thread-safe cache for OAuth access tokens using Caffeine.
 * Stores tokens with per-token expiration time and provides automatic expiration handling.
 */
@Slf4j
public class OAuthTokenCache {

  private static final String CACHE_KEY = "oauth_token";

  /**
   * Wrapper class to store token with its expiration information.
   */
  private static class TokenWithExpiry {
    private final String accessToken;
    private final long expiresInSeconds;
    private final Instant createdAt;

    TokenWithExpiry(String accessToken, long expiresInSeconds) {
      this.accessToken = accessToken;
      this.expiresInSeconds = expiresInSeconds;
      this.createdAt = Instant.now();
    }

    String getAccessToken() {
      return accessToken;
    }

    long getExpiresInSeconds() {
      return expiresInSeconds;
    }

    Instant getExpiresAt() {
      return createdAt.plusSeconds(expiresInSeconds);
    }
  }

  private final Cache<String, TokenWithExpiry> cache;
  private final int refreshBufferSeconds;

  /**
   * Creates a new OAuth token cache.
   *
   * @param refreshBufferSeconds Number of seconds before actual expiration to consider token expired.
   *                            This buffer prevents using tokens that might expire during request processing.
   */
  public OAuthTokenCache(Duration refreshBufferSeconds) {
    this.refreshBufferSeconds = (int) refreshBufferSeconds.toSeconds();

    this.cache = Caffeine.newBuilder()
        .maximumSize(1) // Only one token needed
        .expireAfter(new Expiry<String, TokenWithExpiry>() {
          @Override
          public long expireAfterCreate(String key, TokenWithExpiry value, long currentTime) {
            // Expire after the effective TTL (actual expiration - buffer)
            long effectiveTtl = Math.max(0, value.getExpiresInSeconds());
            log.debug("OAuth token cached: expires in {}s (actual: {}s, buffer: {}s)",
                effectiveTtl, value.getExpiresInSeconds() + refreshBufferSeconds.toSeconds(),
                refreshBufferSeconds.toSeconds());
            return TimeUnit.SECONDS.toNanos(effectiveTtl);
          }

          @Override
          public long expireAfterUpdate(String key, TokenWithExpiry value,
                                        long currentTime, @NonNegative long currentDuration) {
            // Use same logic as create
            return expireAfterCreate(key, value, currentTime);
          }

          @Override
          public long expireAfterRead(String key, TokenWithExpiry value,
                                      long currentTime, @NonNegative long currentDuration) {
            // Don't refresh expiration on read
            return currentDuration;
          }
        })
        .build();

    log.debug("Created OAuth token cache with {}s refresh buffer", refreshBufferSeconds);
  }

  /**
   * Retrieves a valid token from the cache.
   *
   * @return Optional containing the access token if present and not expired, empty otherwise
   */
  public Optional<String> getValidToken() {
    TokenWithExpiry tokenWithExpiry = cache.getIfPresent(CACHE_KEY);

    if (tokenWithExpiry == null) {
      log.debug("OAuth token cache miss: no token cached");
      return Optional.empty();
    }

    long secondsUntilExpiry = tokenWithExpiry.getExpiresAt().getEpochSecond()
        - Instant.now().getEpochSecond();
    log.debug("OAuth token cache hit: token valid for approximately {}s", secondsUntilExpiry);
    return Optional.of(tokenWithExpiry.getAccessToken());
  }

  /**
   * Stores a new token in the cache with calculated expiration time.
   *
   * @param accessToken The OAuth access token
   * @param expiresInSeconds Number of seconds until the token expires (from OAuth server response)
   */
  public void storeToken(String accessToken, long expiresInSeconds) {
    if (accessToken == null || accessToken.isEmpty()) {
      log.warn("Attempted to cache null or empty token, ignoring");
      return;
    }

    if (expiresInSeconds <= 0) {
      log.warn("Attempted to cache token with invalid expiration ({}s), ignoring", expiresInSeconds);
      return;
    }

    // Calculate effective expiration with safety buffer
    long effectiveExpiresIn = Math.max(0, expiresInSeconds - refreshBufferSeconds);

    TokenWithExpiry tokenWithExpiry = new TokenWithExpiry(accessToken, effectiveExpiresIn);
    cache.put(CACHE_KEY, tokenWithExpiry);
  }

  /**
   * Invalidates the cached token, forcing the next request to fetch a fresh token.
   * This is typically called when receiving a 401 Unauthorized response.
   */
  public void invalidate() {
    TokenWithExpiry previous = cache.getIfPresent(CACHE_KEY);
    cache.invalidateAll();

    if (previous != null) {
      log.debug("OAuth token cache invalidated (was valid until {})", previous.getExpiresAt());
    } else {
      log.debug("OAuth token cache invalidation requested, but cache was already empty");
    }
  }

  /**
   * Checks if a token is currently cached (may or may not be expired).
   *
   * @return true if a token is cached, false otherwise
   */
  public boolean hasToken() {
    return cache.getIfPresent(CACHE_KEY) != null;
  }

  /**
   * Gets the expiration time of the currently cached token.
   *
   * @return Optional containing expiration time if token is cached, empty otherwise
   */
  public Optional<Instant> getExpirationTime() {
    TokenWithExpiry tokenWithExpiry = cache.getIfPresent(CACHE_KEY);
    return tokenWithExpiry != null ? Optional.of(tokenWithExpiry.getExpiresAt()) : Optional.empty();
  }
}
