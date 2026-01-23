package io.kafbat.ui.util;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe cache for OAuth access tokens.
 * Stores tokens with expiration time and provides automatic expiration checking.
 */
@Slf4j
public class OAuthTokenCache {

  /**
   * Holds a cached OAuth token with its expiration time.
   */
  private static class CachedToken {
    private final String accessToken;
    private final Instant expiresAt;

    CachedToken(String accessToken, Instant expiresAt) {
      this.accessToken = accessToken;
      this.expiresAt = expiresAt;
    }

    String getAccessToken() {
      return accessToken;
    }

    Instant getExpiresAt() {
      return expiresAt;
    }

    boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }

  private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();
  private final int refreshBufferSeconds;

  /**
   * Creates a new OAuth token cache.
   *
   * @param refreshBufferSeconds Number of seconds before actual expiration to consider token expired.
   *                            This buffer prevents using tokens that might expire during request processing.
   */
  public OAuthTokenCache(int refreshBufferSeconds) {
    this.refreshBufferSeconds = refreshBufferSeconds;
    log.debug("Created OAuth token cache with {}s refresh buffer", refreshBufferSeconds);
  }

  /**
   * Retrieves a valid token from the cache.
   *
   * @return Optional containing the access token if present and not expired, empty otherwise
   */
  public Optional<String> getValidToken() {
    CachedToken token = cachedToken.get();

    if (token == null) {
      log.debug("OAuth token cache miss: no token cached");
      return Optional.empty();
    }

    if (token.isExpired()) {
      long secondsExpired = Instant.now().getEpochSecond() - token.getExpiresAt().getEpochSecond();
      log.debug("OAuth token cache miss: token expired {}s ago", secondsExpired);
      return Optional.empty();
    }

    long secondsUntilExpiry = token.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
    log.debug("OAuth token cache hit: token valid for {}s", secondsUntilExpiry);
    return Optional.of(token.getAccessToken());
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

    // Calculate expiration with safety buffer
    long effectiveExpiresIn = Math.max(0, expiresInSeconds - refreshBufferSeconds);
    Instant expiresAt = Instant.now().plusSeconds(effectiveExpiresIn);

    CachedToken newToken = new CachedToken(accessToken, expiresAt);
    cachedToken.set(newToken);

    log.debug("OAuth token cached: expires at {} (effective TTL: {}s, actual: {}s, buffer: {}s)",
        expiresAt, effectiveExpiresIn, expiresInSeconds, refreshBufferSeconds);
  }

  /**
   * Invalidates the cached token, forcing the next request to fetch a fresh token.
   * This is typically called when receiving a 401 Unauthorized response.
   */
  public void invalidate() {
    CachedToken previous = cachedToken.getAndSet(null);
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
    return cachedToken.get() != null;
  }

  /**
   * Gets the expiration time of the currently cached token.
   *
   * @return Optional containing expiration time if token is cached, empty otherwise
   */
  public Optional<Instant> getExpirationTime() {
    CachedToken token = cachedToken.get();
    return token != null ? Optional.of(token.getExpiresAt()) : Optional.empty();
  }
}
