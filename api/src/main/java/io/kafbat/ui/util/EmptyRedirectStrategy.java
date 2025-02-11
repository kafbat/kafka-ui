package io.kafbat.ui.util;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class EmptyRedirectStrategy implements ServerRedirectStrategy {

  public Mono<Void> sendRedirect(ServerWebExchange exchange, URI location) {
    Assert.notNull(exchange, "exchange cannot be null");
    Assert.notNull(location, "location cannot be null");
    return Mono.fromRunnable(() -> {
      ServerHttpResponse response = exchange.getResponse();
      response.setStatusCode(HttpStatus.FOUND);
      response.getHeaders().setLocation(createLocation(exchange, location));
    });
  }

  private URI createLocation(ServerWebExchange exchange, URI location) {
    String url = location.getPath().isEmpty() ? "/" : location.toASCIIString();

    if (url.startsWith("/")) {
      String context = exchange.getRequest().getPath().contextPath().value();
      return URI.create(context + url);
    }
    return location;
  }
}
