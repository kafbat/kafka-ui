package io.kafbat.ui.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

  private static final String INDEX_HTML = "/static/index.html";

  @GetMapping(value = "/login", produces = {"text/html"})
  public Mono<ClassPathResource> getLoginPage() {
    return Mono.just(new ClassPathResource(INDEX_HTML));
  }

}
