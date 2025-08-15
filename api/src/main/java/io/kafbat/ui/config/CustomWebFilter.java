package io.kafbat.ui.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CustomWebFilter implements WebFilter {

  @Override
  public @NotNull Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {

    final String basePath = exchange.getRequest().getPath().contextPath().value();

    final String path = exchange.getRequest().getPath().pathWithinApplication().value();

    ServerWebExchange filterExchange = exchange;

    if (path.startsWith("/ui") || path.isEmpty() || path.equals("/")) {
      filterExchange = exchange.mutate().request(
          exchange.getRequest().mutate()
              .path(basePath + "/index.html")
              .contextPath(basePath)
              .build()
      ).build();
    }

    return chain.filter(filterExchange).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange));
  }
}
