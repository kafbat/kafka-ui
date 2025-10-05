package io.kafbat.ui.util.gcp;

import com.google.cloud.hosted.kafka.auth.GcpBearerAuthCredentialProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class GcpBearerAuthFilter implements ExchangeFilterFunction {

  private static final String GCP_BEARER_AUTH_CUSTOM_PROVIDER_CLASS =
      GcpBearerAuthCredentialProvider.class.getName();

  private final GcpBearerAuthCredentialProvider credentialProvider;

  public GcpBearerAuthFilter() {
    this.credentialProvider = new GcpBearerAuthCredentialProvider();
  }

  @NotNull
  @Override
  public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
    // This Mono ensures token fetching happens for EACH request
    return Mono.fromCallable(() -> this.credentialProvider.getBearerToken(null))
    .flatMap(token -> {
      // Create a new request with the Authorization header
      ClientRequest newRequest = ClientRequest.from(request)
          .headers(headers -> headers.setBearerAuth(token))
          .build();
      // Pass the new request to the next filter in the chain
      return next.exchange(newRequest);
    });
  }

  public static String getGcpBearerAuthCustomProviderClass() {
    return GCP_BEARER_AUTH_CUSTOM_PROVIDER_CLASS;
  }
}
