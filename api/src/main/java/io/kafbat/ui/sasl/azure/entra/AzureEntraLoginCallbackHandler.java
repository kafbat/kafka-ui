package io.kafbat.ui.sasl.azure.entra;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureEntraLoginCallbackHandler implements AuthenticateCallbackHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AzureEntraLoginCallbackHandler.class);

  private static final String TOKEN_AUDIENCE_FORMAT = "%s://%s/.default";

  static TokenCredential tokenCredential = new DefaultAzureCredentialBuilder().build();

  private TokenRequestContext tokenRequestContext;

  @Override
  public void configure(
      Map<String, ?> configs, String mechanism, List<AppConfigurationEntry> jaasConfigEntries) {
    tokenRequestContext = buildTokenRequestContext(configs);
  }

  private TokenRequestContext buildTokenRequestContext(Map<String, ?> configs) {
    URI uri = buildEventHubsServerUri(configs);
    String tokenAudience = buildTokenAudience(uri);

    TokenRequestContext request = new TokenRequestContext();
    request.addScopes(tokenAudience);
    return request;
  }

  private URI buildEventHubsServerUri(Map<String, ?> configs) {
    final List<String> bootstrapServers = (List<String>) configs.get(BOOTSTRAP_SERVERS_CONFIG);

    if (null == bootstrapServers) {
      final String message = BOOTSTRAP_SERVERS_CONFIG + " is missing from the Kafka configuration.";
      LOGGER.error(message);
      throw new IllegalArgumentException(message);
    }

    if (bootstrapServers.size() != 1) {
      final String message =
          BOOTSTRAP_SERVERS_CONFIG
              + " contains multiple bootstrap servers. Only a single bootstrap server is supported.";
      LOGGER.error(message);
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
      if (callback instanceof OAuthBearerTokenCallback oauthCallback) {
        handleOAuthCallback(oauthCallback);
      } else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }

  private void handleOAuthCallback(OAuthBearerTokenCallback oauthCallback) {
    try {
      final AccessToken accessToken = tokenCredential.getTokenSync(tokenRequestContext);
      final AzureEntraOAuthBearerTokenImpl oauthBearerToken = new AzureEntraOAuthBearerTokenImpl(accessToken);
      oauthCallback.token(oauthBearerToken);
    } catch (final RuntimeException e) {
      final String message =
          "Failed to acquire Azure token for Event Hub Authentication. "
              + "Please ensure valid Azure credentials are configured.";
      LOGGER.error(message, e);
      oauthCallback.error("invalid_grant", message, null);
    }
  }

  public void close() {
    // NOOP
  }

  void setTokenCredential(final TokenCredential tokenCredential) {
    AzureEntraLoginCallbackHandler.tokenCredential = tokenCredential;
  }
}
