package io.kafbat.ui.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kafbat.ui.AbstractIntegrationTest;
import io.kafbat.ui.config.auth.RbacUser;
import io.kafbat.ui.config.auth.RoleBasedAccessControlProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebFilter;
import reactor.core.Disposable;

/**
 * RBAC must apply to MCP tool calls the same way it applies to the REST API: the tool runs with the
 * caller's authentication, so a user without access to a cluster is denied.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "mcp.enabled=true")
@Import(McpRbacTest.HeaderAuthentication.class)
class McpRbacTest extends AbstractIntegrationTest {

  private static final String USER_HEADER = "X-Test-User";
  private static final String ROLE_NAME = "local-viewer";

  @Autowired
  WebTestClient webTestClient;

  @Autowired
  AccessControlService accessControlService;

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Authenticates by a header the way Spring Security's authentication filters do: the
   * Authentication is written into the Reactor context of the rest of the filter chain.
   */
  @TestConfiguration
  static class HeaderAuthentication {
    @Bean
    @Order(0)
    WebFilter testHeaderAuthentication() {
      return (exchange, chain) -> {
        String name = exchange.getRequest().getHeaders().getFirst(USER_HEADER);
        if (name == null) {
          return chain.filter(exchange);
        }
        List<String> groups = "bob".equals(name) ? List.of(ROLE_NAME) : List.of();
        var authentication = new UsernamePasswordAuthenticationToken(new TestUser(name, groups), null, List.of());
        return chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
      };
    }
  }

  record TestUser(String name, Collection<String> groups) implements RbacUser {
  }

  @BeforeEach
  void enableRbac() {
    Role role = new Role();
    role.setName(ROLE_NAME);
    role.setClusters(List.of(LOCAL));
    role.setPermissions(List.of());

    RoleBasedAccessControlProperties properties = mock();
    when(properties.getRoles()).thenReturn(List.of(role));
    ReflectionTestUtils.setField(accessControlService, "properties", properties);
    ReflectionTestUtils.setField(accessControlService, "rbacEnabled", true);
  }

  @Test
  void toolCallIsDeniedForUserWithoutAccessToCluster() throws Exception {
    JsonNode result = callGetBrokers("alice");

    assertThat(result.path("isError").asBoolean()).as(result.toString()).isTrue();
    assertThat(result.toString()).contains("Access denied");
  }

  @Test
  void toolCallIsAllowedForUserWithAccessToCluster() throws Exception {
    JsonNode result = callGetBrokers("bob");

    assertThat(result.path("isError").asBoolean()).as(result.toString()).isFalse();
  }

  private JsonNode callGetBrokers(String user) throws Exception {
    BlockingQueue<ServerSentEvent<String>> events = new LinkedBlockingQueue<>();
    Disposable stream = webTestClient.get().uri("/mcp/sse")
        .header(USER_HEADER, user)
        .accept(MediaType.TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isOk()
        .returnResult(new ParameterizedTypeReference<ServerSentEvent<String>>() {
        })
        .getResponseBody()
        .subscribe(events::add);
    try {
      String endpoint = nextEvent(events, "endpoint").data();
      post(endpoint, user, """
          {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05",
           "capabilities":{},"clientInfo":{"name":"test","version":"1"}}}""");
      nextEvent(events, "message");
      post(endpoint, user, """
          {"jsonrpc":"2.0","method":"notifications/initialized"}""");
      post(endpoint, user, """
          {"jsonrpc":"2.0","id":2,"method":"tools/call",
           "params":{"name":"getBrokers","arguments":{"clusterName":"%s"}}}""".formatted(LOCAL));
      return mapper.readTree(nextEvent(events, "message").data()).path("result");
    } finally {
      stream.dispose();
    }
  }

  private void post(String endpoint, String user, String body) {
    webTestClient.post().uri(endpoint)
        .header(USER_HEADER, user)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus().is2xxSuccessful();
  }

  private static ServerSentEvent<String> nextEvent(BlockingQueue<ServerSentEvent<String>> events, String type)
      throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      ServerSentEvent<String> event = events.poll(1, TimeUnit.SECONDS);
      if (event != null && type.equals(event.event())) {
        return event;
      }
    }
    throw new AssertionError("no '" + type + "' event within 30s");
  }
}
