package io.kafbat.ui.config.auth.azure;

import com.azure.core.credential.AccessToken;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

public class AzureEntraOAuthBearerToken implements OAuthBearerToken {

  private final AccessToken accessToken;
  private final JWTClaimsSet claims;

  public AzureEntraOAuthBearerToken(AccessToken accessToken) {
    this.accessToken = accessToken;

    try {
      claims = JWTParser.parse(accessToken.getToken()).getJWTClaimsSet();
    } catch (ParseException exception) {
      throw new SaslAuthenticationException("Unable to parse the access token", exception);
    }
  }

  @Override
  public String value() {
    return accessToken.getToken();
  }

  @Override
  public Long startTimeMs() {
    return claims.getIssueTime().getTime();
  }

  @Override
  public long lifetimeMs() {
    return claims.getExpirationTime().getTime();
  }

  @Override
  public Set<String> scope() {
    // Referring to
    // https://docs.microsoft.com/azure/active-directory/develop/access-tokens#payload-claims, the
    // scp
    // claim is a String which is presented as a space separated list.
    return Arrays
        .stream(((String) claims.getClaim("scp")).split(" "))
        .collect(Collectors.toSet());
  }

  @Override
  public String principalName() {
    return (String) claims.getClaim("upn");
  }

  public boolean isExpired() {
    return accessToken.isExpired();
  }
}
