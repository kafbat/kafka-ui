package io.kafbat.ui.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response model for OAuth 2.0 token endpoint (client credentials flow).
 * Represents the standard OAuth token response as defined in RFC 6749.
 */
@Data
public class OAuthTokenResponse {

  /**
   * The access token issued by the authorization server.
   * This is the Bearer token to be used in API requests.
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * The type of the token issued.
   * Typically "Bearer" for OAuth 2.0.
   */
  @JsonProperty("token_type")
  private String tokenType;

  /**
   * The lifetime in seconds of the access token.
   * For example, the value "3600" denotes that the access token will expire in one hour.
   */
  @JsonProperty("expires_in")
  private Long expiresIn;

  /**
   * The scope of the access token (optional).
   * Space-separated list of scopes.
   */
  @JsonProperty("scope")
  private String scope;

  /**
   * Refresh token (optional, not typically used in client credentials flow).
   */
  @JsonProperty("refresh_token")
  private String refreshToken;

  /**
   * Checks if the response contains a valid access token.
   *
   * @return true if access token is present and not empty
   */
  public boolean hasAccessToken() {
    return accessToken != null && !accessToken.isEmpty();
  }

  /**
   * Checks if the response contains expiration information.
   *
   * @return true if expires_in is present and positive
   */
  public boolean hasExpiresIn() {
    return expiresIn != null && expiresIn > 0;
  }
}
