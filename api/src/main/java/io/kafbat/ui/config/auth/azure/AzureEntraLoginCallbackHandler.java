package io.kafbat.ui.config.auth.azure;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

@Slf4j
public class AzureEntraLoginCallbackHandler implements AuthenticateCallbackHandler {

  private static final Duration ACCESS_TOKEN_REQUEST_BLOCK_TIME = Duration.ofSeconds(10);

  private static final int ACCESS_TOKEN_REQUEST_MAX_RETRIES = 6;

  private static final String TOKEN_AUDIENCE_FORMAT = "%s://%s/.default";

  static TokenCredential tokenCredential = new DefaultAzureCredentialBuilder().build();

  private TokenRequestContext tokenRequestContext;

  @Override
  public void configure(Map<String, ?> configs,
                        String mechanism,
                        List<AppConfigurationEntry> jaasConfigEntries) {
    tokenRequestContext = buildTokenRequestContext(configs);
  }

  private TokenRequestContext buildTokenRequestContext(Map<String, ?> configs) {
    URI uri = buildEventHubsServerUri(configs);
    String tokenAudience = buildTokenAudience(uri);

    TokenRequestContext request = new TokenRequestContext();
    request.addScopes(tokenAudience);
    return request;
  }

  @SuppressWarnings("unchecked")
  private URI buildEventHubsServerUri(Map<String, ?> configs) {
    final List<String> bootstrapServers = (List<String>) configs.get(BOOTSTRAP_SERVERS_CONFIG);

    if (bootstrapServers == null) {
      final String message = BOOTSTRAP_SERVERS_CONFIG + " is missing from the Kafka configuration.";
      log.error(message);
      throw new IllegalArgumentException(message);
    }

    if (bootstrapServers.size() > 1) {
      final String message =
          BOOTSTRAP_SERVERS_CONFIG
              + " contains multiple bootstrap servers. Only a single bootstrap server is supported.";
      log.error(message);
      throw new IllegalArgumentException(message);
    }

    return URI.create("https://" + bootstrapServers.get(0));
  }

  private String buildTokenAudience(URI uri) {
    return String.format(TOKEN_AUDIENCE_FORMAT, uri.getScheme(), uri.getHost());
  }

  @Override
  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
    for (Callback callback : callbacks) {
      if (!(callback instanceof OAuthBearerTokenCallback oauthCallback)) {
        throw new UnsupportedCallbackException(callback);
      }
      handleOAuthCallback(oauthCallback);
    }
  }

  private void handleOAuthCallback(OAuthBearerTokenCallback oauthCallback) {
    try {
      final OAuthBearerToken token = tokenCredential
          .getToken(tokenRequestContext)
          .map(AzureEntraOAuthBearerToken::new)
          .timeout(ACCESS_TOKEN_REQUEST_BLOCK_TIME)
          .doOnError(e -> log.warn("Failed to acquire Azure token for Event Hub Authentication. Retrying.", e))
          .retry(ACCESS_TOKEN_REQUEST_MAX_RETRIES)
          .block();

      oauthCallback.token(token);
    } catch (RuntimeException e) {
      final String message =
          "Failed to acquire Azure token for Event Hub Authentication. "
              + "Please ensure valid Azure credentials are configured.";
      log.error(message, e);
      oauthCallback.error("invalid_grant", message, null);
    }
  }

  public void close() {
    // NOOP
  }

  void setTokenCredential(TokenCredential tokenCredential) {
    AzureEntraLoginCallbackHandler.tokenCredential = tokenCredential;
  }
}
