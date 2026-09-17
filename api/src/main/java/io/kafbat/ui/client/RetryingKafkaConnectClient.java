package io.kafbat.ui.client;

import static org.apache.commons.lang3.Strings.CI;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kafbat.ui.config.ClustersProperties;
import io.kafbat.ui.connect.ApiClient;
import io.kafbat.ui.connect.api.KafkaConnectClientApi;
import io.kafbat.ui.connect.model.Connector;
import io.kafbat.ui.connect.model.ConnectorExpand;
import io.kafbat.ui.connect.model.ConnectorPlugin;
import io.kafbat.ui.connect.model.ConnectorPluginConfigValidationResponse;
import io.kafbat.ui.connect.model.ConnectorStatus;
import io.kafbat.ui.connect.model.ConnectorTask;
import io.kafbat.ui.connect.model.ConnectorTopics;
import io.kafbat.ui.connect.model.ExpandedConnector;
import io.kafbat.ui.connect.model.NewConnector;
import io.kafbat.ui.connect.model.TaskStatus;
import io.kafbat.ui.exception.KafkaConnectConflictResponseException;
import io.kafbat.ui.exception.ValidationException;
import io.kafbat.ui.util.WebClientConfigurator;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
public class RetryingKafkaConnectClient extends KafkaConnectClientApi {
  private static final int MAX_RETRIES = 5;
  private static final Duration RETRIES_DELAY = Duration.ofMillis(200);

  public RetryingKafkaConnectClient(ClustersProperties.ConnectCluster config,
                                    @Nullable ClustersProperties.TruststoreConfig truststoreConfig,
                                    DataSize maxBuffSize,
                                    Duration responseTimeout) {
    super(new RetryingApiClient(config, truststoreConfig, maxBuffSize, responseTimeout));
  }

  private static Retry conflictCodeRetry() {
    return Retry
        .fixedDelay(MAX_RETRIES, RETRIES_DELAY)
        .filter(e -> e instanceof WebClientResponseException.Conflict)
        .onRetryExhaustedThrow((spec, signal) ->
            new KafkaConnectConflictResponseException(
                (WebClientResponseException.Conflict) signal.failure()));
  }

  private static @NotNull Retry retryOnRebalance() {
    return Retry.fixedDelay(MAX_RETRIES, RETRIES_DELAY).filter(e -> {

      if (e instanceof WebClientResponseException.InternalServerError exception) {
        final var errorMessage = getMessage(exception);
        return CI.equals(errorMessage,
            // From https://github.com/apache/kafka/blob/dfc07e0e0c6e737a56a5402644265f634402b864/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/distributed/DistributedHerder.java#L2340
            "Request cannot be completed because a rebalance is expected");
      }
      return false;
    });
  }

  private static <T> Mono<T> withRetryOnConflictOrRebalance(Mono<T> publisher) {
    return publisher
        .retryWhen(retryOnRebalance())
        .retryWhen(conflictCodeRetry());
  }

  private static <T> Flux<T> withRetryOnConflictOrRebalance(Flux<T> publisher) {
    return publisher
        .retryWhen(retryOnRebalance())
        .retryWhen(conflictCodeRetry());
  }

  private static <T> Mono<T> withRetryOnRebalance(Mono<T> publisher) {
    return publisher.retryWhen(retryOnRebalance());
  }


  private static <T> Mono<T> withBadRequestErrorHandling(Mono<T> publisher) {
    return publisher
        .onErrorResume(WebClientResponseException.BadRequest.class,
            RetryingKafkaConnectClient::parseConnectErrorMessage)
        .onErrorResume(WebClientResponseException.InternalServerError.class,
            RetryingKafkaConnectClient::parseConnectErrorMessage);
  }

  // Adapted from https://github.com/apache/kafka/blob/a0a501952b6d61f6f273bdb8f842346b51e9dfce/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/rest/entities/ErrorMessage.java
  // Adding the connect runtime dependency for this single class seems excessive
  private record ErrorMessage(@NotNull @JsonProperty("message") String message) {
  }

  private static <T> @NotNull Mono<T> parseConnectErrorMessage(WebClientResponseException parseException) {
    return Mono.error(new ValidationException(getMessage(parseException)));
  }

  private static String getMessage(WebClientResponseException parseException) {
    final var errorMessage = parseException.getResponseBodyAs(ErrorMessage.class);
    return Objects.requireNonNull(errorMessage,
            // see https://github.com/apache/kafka/blob/a0a501952b6d61f6f273bdb8f842346b51e9dfce/connect/runtime/src/main/java/org/apache/kafka/connect/runtime/rest/errors/ConnectExceptionMapper.java
            "This should not happen according to the ConnectExceptionMapper")
        .message();
  }

  @Override
  public Mono<Connector> createConnector(NewConnector newConnector) throws RestClientException {
    return withBadRequestErrorHandling(
        withRetryOnRebalance(super.createConnector(newConnector))
    );
  }

  @Override
  public Mono<Connector> setConnectorConfig(String connectorName, Map<String, Object> requestBody)
      throws RestClientException {
    return withBadRequestErrorHandling(
        withRetryOnRebalance(super.setConnectorConfig(connectorName, requestBody))
    );
  }

  @Override
  public Mono<ResponseEntity<Connector>> createConnectorWithHttpInfo(NewConnector newConnector)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.createConnectorWithHttpInfo(newConnector));
  }

  @Override
  public Mono<Void> deleteConnector(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.deleteConnector(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Void>> deleteConnectorWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.deleteConnectorWithHttpInfo(connectorName));
  }


  @Override
  public Mono<Connector> getConnector(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnector(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Connector>> getConnectorWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Map<String, Object>> getConnectorConfig(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorConfig(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Map<String, Object>>> getConnectorConfigWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorConfigWithHttpInfo(connectorName));
  }

  @Override
  public Flux<ConnectorPlugin> getConnectorPlugins() throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorPlugins());
  }

  @Override
  public Mono<ResponseEntity<List<ConnectorPlugin>>> getConnectorPluginsWithHttpInfo()
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorPluginsWithHttpInfo());
  }

  @Override
  public Mono<ConnectorStatus> getConnectorStatus(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorStatus(connectorName));
  }

  @Override
  public Mono<ResponseEntity<ConnectorStatus>> getConnectorStatusWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorStatusWithHttpInfo(connectorName));
  }

  @Override
  public Mono<TaskStatus> getConnectorTaskStatus(String connectorName, Integer taskId)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTaskStatus(connectorName, taskId));
  }

  @Override
  public Mono<ResponseEntity<TaskStatus>> getConnectorTaskStatusWithHttpInfo(String connectorName, Integer taskId)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTaskStatusWithHttpInfo(connectorName, taskId));
  }

  @Override
  public Flux<ConnectorTask> getConnectorTasks(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTasks(connectorName));
  }

  @Override
  public Mono<ResponseEntity<List<ConnectorTask>>> getConnectorTasksWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTasksWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Map<String, ConnectorTopics>> getConnectorTopics(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTopics(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Map<String, ConnectorTopics>>> getConnectorTopicsWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorTopicsWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Map<String, ExpandedConnector>> getConnectors(
      String search, List<ConnectorExpand> expand
  ) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectors(search, expand));
  }

  @Override
  public Mono<ResponseEntity<Map<String, ExpandedConnector>>> getConnectorsWithHttpInfo(
      String search, List<ConnectorExpand> expand
  ) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.getConnectorsWithHttpInfo(search, expand));
  }

  @Override
  public Mono<Void> pauseConnector(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.pauseConnector(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Void>> pauseConnectorWithHttpInfo(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.pauseConnectorWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Void> stopConnector(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.stopConnector(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Void>> stopConnectorWithHttpInfo(String connectorName) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.stopConnectorWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Void> restartConnector(String connectorName, Boolean includeTasks, Boolean onlyFailed)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.restartConnector(connectorName, includeTasks, onlyFailed));
  }

  @Override
  public Mono<ResponseEntity<Void>> restartConnectorWithHttpInfo(String connectorName, Boolean includeTasks,
                                                                 Boolean onlyFailed) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.restartConnectorWithHttpInfo(connectorName, includeTasks, onlyFailed));
  }

  @Override
  public Mono<Void> restartConnectorTask(String connectorName, Integer taskId) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.restartConnectorTask(connectorName, taskId));
  }

  @Override
  public Mono<ResponseEntity<Void>> restartConnectorTaskWithHttpInfo(String connectorName, Integer taskId)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.restartConnectorTaskWithHttpInfo(connectorName, taskId));
  }

  @Override
  public Mono<Void> resetConnectorOffsets(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.resetConnectorOffsets(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Void>> resetConnectorOffsetsWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.resetConnectorOffsetsWithHttpInfo(connectorName));
  }

  @Override
  public Mono<Void> resumeConnector(String connectorName) throws WebClientResponseException {
    return withRetryOnRebalance(super.resumeConnector(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Void>> resumeConnectorWithHttpInfo(String connectorName)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.resumeConnectorWithHttpInfo(connectorName));
  }

  @Override
  public Mono<ResponseEntity<Connector>> setConnectorConfigWithHttpInfo(String connectorName,
                                                                        Map<String, Object> requestBody)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.setConnectorConfigWithHttpInfo(connectorName, requestBody));
  }

  @Override
  public Mono<ConnectorPluginConfigValidationResponse> validateConnectorPluginConfig(String pluginName,
                                                                                     Map<String, Object> requestBody)
      throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.validateConnectorPluginConfig(pluginName, requestBody));
  }

  @Override
  public Mono<ResponseEntity<ConnectorPluginConfigValidationResponse>> validateConnectorPluginConfigWithHttpInfo(
      String pluginName, Map<String, Object> requestBody) throws WebClientResponseException {
    return withRetryOnConflictOrRebalance(super.validateConnectorPluginConfigWithHttpInfo(pluginName, requestBody));
  }

  private static class RetryingApiClient extends ApiClient {

    public RetryingApiClient(ClustersProperties.ConnectCluster config,
                             ClustersProperties.TruststoreConfig truststoreConfig,
                             DataSize maxBuffSize,
                             Duration responseTimeout) {
      super(buildWebClient(maxBuffSize, responseTimeout, config, truststoreConfig), null, null);
      setBasePath(config.getAddress());
      setUsername(config.getUsername());
      setPassword(config.getPassword());
    }

    public static WebClient buildWebClient(DataSize maxBuffSize,
                                           Duration responseTimeout,
                                           ClustersProperties.ConnectCluster config,
                                           ClustersProperties.TruststoreConfig truststoreConfig) {
      return new WebClientConfigurator()
          .configureSsl(
              truststoreConfig,
              new ClustersProperties.KeystoreConfig(
                  config.getKeystoreType(),
                  config.getKeystoreCertificate(),
                  config.getKeystoreLocation(),
                  config.getKeystorePassword()
              )
          )
          .configureBasicAuth(
              config.getUsername(),
              config.getPassword()
          )
          .configureBufferSize(maxBuffSize)
          .configureResponseTimeout(responseTimeout)
          .build();
    }
  }
}
