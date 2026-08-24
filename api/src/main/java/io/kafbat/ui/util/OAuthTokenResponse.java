package io.kafbat.ui.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OAuthTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("token_type")
  private String tokenType;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("scope")
  private String scope;

  @JsonProperty("refresh_token")
  private String refreshToken;

  public boolean hasAccessToken() {
    return accessToken != null && !accessToken.isEmpty();
  }

  public boolean hasExpiresIn() {
    return expiresIn != null && expiresIn > 0;
  }
}
