package io.kafbat.ui.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class CorsGlobalConfiguration {

  @Autowired
  private CorsProperties corsProperties;

  @Bean
  public WebFilter corsFilter() {
    return (final ServerWebExchange ctx, final WebFilterChain chain) -> {
      final ServerHttpRequest request = ctx.getRequest();

      final ServerHttpResponse response = ctx.getResponse();
      final HttpHeaders headers = response.getHeaders();
      headers.add("Access-Control-Allow-Origin", corsProperties.getAllowedOrigins());
      headers.add("Access-Control-Allow-Methods", corsProperties.getAllowedMethods());
      headers.add("Access-Control-Max-Age", corsProperties.getMaxAge());
      headers.add("Access-Control-Allow-Headers", corsProperties.getAllowedHeaders());

      if (request.getMethod() == HttpMethod.OPTIONS) {
        response.setStatusCode(HttpStatus.OK);
        return Mono.empty();
      }

      return chain.filter(ctx);
    };
  }

}
