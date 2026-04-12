package io.kafbat.ui.config.auth.azure;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.auth.SaslExtensions;
import org.apache.kafka.common.security.auth.SaslExtensionsCallback;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

@Slf4j
public class AzureEntraLoginCallbackHandler implements AuthenticateCallbackHandler {

  private static final Duration ACCESS_TOKEN_REQUEST_BLOCK_TIME = Duration.ofSeconds(10);

  private static final int ACCESS_TOKEN_REQUEST_MAX_RETRIES = 6;

  private static final String TOKEN_AUDIENCE_FORMAT = "%s://%s/.default";

  /**
   * JAAS config option to override the token scope. When set, this value is used
   * instead of deriving the scope from the bootstrap server URL. This is required
   * for services like Confluent Cloud where the OAuth scope is an application
   * registration URI (e.g. {@code api://<client-id>/.default}) rather than the
   * bootstrap server hostname.
   */
  static final String JAAS_OPTION_SCOPE = "scope";

  /**
   * Prefix for JAAS config options that should be forwarded as SASL extensions.
   * For example, {@code extension_logicalCluster="lkc-xxxxx"} in the JAAS config
   * becomes the SASL extension {@code logicalCluster=lkc-xxxxx}. This is required
   * by Confluent Cloud to route OAUTHBEARER requests to the correct logical cluster
   * and identity pool.
   */
  static final String EXTENSION_PREFIX = "extension_";

  static TokenCredential tokenCredential = new DefaultAzureCredentialBuilder().build();

  private TokenRequestContext tokenRequestContext;

  private SaslExtensions saslExtensions = SaslExtensions.empty();

  @Override
  public void configure(Map<String, ?> configs,
                        String mechanism,
                        List<AppConfigurationEntry> jaasConfigEntries) {
    tokenRequestContext = buildTokenRequestContext(configs, jaasConfigEntries);
    saslExtensions = buildSaslExtensions(jaasConfigEntries);
  }

  private TokenRequestContext buildTokenRequestContext(Map<String, ?> configs,
                                                       List<AppConfigurationEntry> jaasConfigEntries) {
    String customScope = getJaasOption(jaasConfigEntries, JAAS_OPTION_SCOPE);

    String tokenAudience;
    if (customScope != null) {
      log.info("Using custom OAuth scope from JAAS config: {}", customScope);
      tokenAudience = customScope;
    } else {
      URI uri = buildBootstrapServerUri(configs);
      tokenAudience = buildTokenAudience(uri);
    }

    TokenRequestContext request = new TokenRequestContext();
    request.addScopes(tokenAudience);
    return request;
  }

  @Nullable
  private String getJaasOption(List<AppConfigurationEntry> jaasConfigEntries, String key) {
    if (jaasConfigEntries == null) {
      return null;
    }
    for (AppConfigurationEntry entry : jaasConfigEntries) {
      Object value = entry.getOptions().get(key);
      if (value instanceof String s) {
        String trimmed = s.trim();
        if (!trimmed.isEmpty()) {
          return trimmed;
        }
      }
    }
    return null;
  }

  /**
   * Extracts SASL extensions from JAAS config options with the {@code extension_} prefix.
   * These extensions are sent to the broker during the SASL/OAUTHBEARER handshake.
   * Confluent Cloud requires {@code logicalCluster} and {@code identityPoolId} extensions
   * to route requests to the correct cluster and identity pool.
   */
  private SaslExtensions buildSaslExtensions(List<AppConfigurationEntry> jaasConfigEntries) {
    if (jaasConfigEntries == null) {
      return SaslExtensions.empty();
    }
    Map<String, String> extensions = new HashMap<>();
    for (AppConfigurationEntry entry : jaasConfigEntries) {
      for (Map.Entry<String, ?> option : entry.getOptions().entrySet()) {
        if (option.getKey().startsWith(EXTENSION_PREFIX) && option.getValue() instanceof String value) {
          String extensionName = option.getKey().substring(EXTENSION_PREFIX.length());
          String trimmed = value.trim();
          if (!extensionName.isEmpty() && !trimmed.isEmpty()) {
            extensions.put(extensionName, trimmed);
          }
        }
      }
    }
    if (!extensions.isEmpty()) {
      log.info("Loaded {} SASL extension(s) from JAAS config: {}", extensions.size(), extensions.keySet());
    }
    return new SaslExtensions(extensions);
  }

  @SuppressWarnings("unchecked")
  private URI buildBootstrapServerUri(Map<String, ?> configs) {
    final List<String> bootstrapServers = (List<String>) configs.get(BOOTSTRAP_SERVERS_CONFIG);

    if (bootstrapServers == null) {
      final String message = BOOTSTRAP_SERVERS_CONFIG + " is missing from the Kafka configuration.";
      log.error(message);
      throw new IllegalArgumentException(message);
    }

    if (bootstrapServers.size() != 1) {
      final String message =
          BOOTSTRAP_SERVERS_CONFIG
              + " must contain exactly one bootstrap server, but found " + bootstrapServers.size() + ".";
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
      if (callback instanceof OAuthBearerTokenCallback oauthCallback) {
        handleOAuthCallback(oauthCallback);
      } else if (callback instanceof SaslExtensionsCallback extensionsCallback) {
        extensionsCallback.extensions(saslExtensions);
      } else {
        throw new UnsupportedCallbackException(callback);
      }
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

      if (token == null) {
        oauthCallback.error("invalid_grant", "Token acquisition returned empty result.", null);
        return;
      }
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
