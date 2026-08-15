package io.kafbat.ui.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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

  private static List<String> allowedOrigins = List.of("*");

  @Value("${auth.cors.allowed-origins:*}")
  public void setAllowedOrigins(List<String> allowedOrigins) {
    CorsGlobalConfiguration.allowedOrigins = allowedOrigins;
  }

  @Bean
  public WebFilter corsFilter() {
    return (final ServerWebExchange ctx, final WebFilterChain chain) -> {
      final ServerHttpRequest request = ctx.getRequest();

      final ServerHttpResponse response = ctx.getResponse();
      final HttpHeaders headers = response.getHeaders();
      fillCorsHeader(headers, request);

      if (request.getMethod() == HttpMethod.OPTIONS) {
        response.setStatusCode(HttpStatus.OK);
        return Mono.empty();
      }

      return chain.filter(ctx);
    };
  }

  public static void fillCorsHeader(HttpHeaders responseHeaders, ServerHttpRequest request) {
    String origin = request.getHeaders().getOrigin();
    if (origin != null) {
      if (allowedOrigins.contains("*") || allowedOrigins.contains(origin)) {
        responseHeaders.add("Access-Control-Allow-Origin", origin);
        responseHeaders.add("Access-Control-Allow-Credentials", "true");
      }
    }
    responseHeaders.add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
    responseHeaders.add("Access-Control-Max-Age", "3600");
    responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
  }
}
