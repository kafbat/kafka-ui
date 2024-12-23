package io.kafbat.ui.util;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class StaticFileWebFilter implements WebFilter {

  private final ServerWebExchangeMatcher matcher;
  private final String contents;

  public StaticFileWebFilter(String path, ClassPathResource resource) {
    this.matcher = ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, path);

    try {
      this.contents = ResourceUtil.readAsString(resource);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @NotNull Mono<Void> filter(@NotNull ServerWebExchange exchange, WebFilterChain chain) {
    return this.matcher.matches(exchange)
        .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
        .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
        .flatMap((matchResult) -> this.render(exchange));
  }

  private Mono<Void> render(ServerWebExchange exchange) {
    String contextPath = exchange.getRequest().getPath().contextPath().value();

    String contentBody = contents
        .replace("\"assets/", "\"" + contextPath + "/assets/")
        .replace("PUBLIC-PATH-VARIABLE", contextPath);

    ServerHttpResponse result = exchange.getResponse();
    result.setStatusCode(HttpStatus.OK);
    result.getHeaders().setContentType(MediaType.TEXT_HTML);
    DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
    return result.writeWith(Mono.just(bufferFactory.wrap(contentBody.getBytes())));
  }

}
