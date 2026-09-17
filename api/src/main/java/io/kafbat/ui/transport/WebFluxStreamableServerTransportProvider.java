/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.kafbat.ui.transport;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityException;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransport;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.ProtocolVersions;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Vendored from Spring AI 2.0.0-M4 (org.springframework.ai:mcp-spring-webflux)
 * with compatibility fix for Spring Framework 6.2 (HttpHeaders.asMultiValueMap() -> new LinkedHashMap()).
 *
 * @author Dariusz Jędrzejczyk
 * @author Christian Tzolov
 */
public final class WebFluxStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

	private static final Logger logger = LoggerFactory.getLogger(WebFluxStreamableServerTransportProvider.class);

	public static final String MESSAGE_EVENT_TYPE = "message";

	private final McpJsonMapper jsonMapper;

	private final String mcpEndpoint;

	private final boolean disallowDelete;

	private final RouterFunction<?> routerFunction;

	private McpStreamableServerSession.Factory sessionFactory;

	private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

	private McpTransportContextExtractor<ServerRequest> contextExtractor;

	private volatile boolean isClosing = false;

	private KeepAliveScheduler keepAliveScheduler;

	private final ServerTransportSecurityValidator securityValidator;

	private WebFluxStreamableServerTransportProvider(McpJsonMapper jsonMapper, String mcpEndpoint,
			McpTransportContextExtractor<ServerRequest> contextExtractor, boolean disallowDelete,
			Duration keepAliveInterval, ServerTransportSecurityValidator securityValidator) {
		Assert.notNull(jsonMapper, "JsonMapper must not be null");
		Assert.notNull(mcpEndpoint, "Message endpoint must not be null");
		Assert.notNull(contextExtractor, "Context extractor must not be null");
		Assert.notNull(securityValidator, "Security validator must not be null");

		this.jsonMapper = jsonMapper;
		this.mcpEndpoint = mcpEndpoint;
		this.contextExtractor = contextExtractor;
		this.disallowDelete = disallowDelete;
		this.securityValidator = securityValidator;
		this.routerFunction = RouterFunctions.route()
			.GET(this.mcpEndpoint, this::handleGet)
			.POST(this.mcpEndpoint, this::handlePost)
			.DELETE(this.mcpEndpoint, this::handleDelete)
			.build();

		if (keepAliveInterval != null) {
			this.keepAliveScheduler = KeepAliveScheduler
				.builder(() -> (this.isClosing) ? Flux.empty() : Flux.fromIterable(this.sessions.values()))
				.initialDelay(keepAliveInterval)
				.interval(keepAliveInterval)
				.build();

			this.keepAliveScheduler.start();
		}
	}

	/**
	 * Spring 6.2 compatible replacement for HttpHeaders.asMultiValueMap() (Spring 7.0+).
	 * HttpHeaders implements MultiValueMap in Spring 6.2, so we convert to a plain Map.
	 */
	private static Map<String, List<String>> toHeaderMap(org.springframework.http.HttpHeaders httpHeaders) {
		return new LinkedHashMap<>(httpHeaders);
	}

	@Override
	public List<String> protocolVersions() {
		return List.of(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26,
				ProtocolVersions.MCP_2025_06_18, ProtocolVersions.MCP_2025_11_25);
	}

	@Override
	public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public Mono<Void> notifyClients(String method, Object params) {
		if (this.sessions.isEmpty()) {
			logger.debug("No active sessions to broadcast message to");
			return Mono.empty();
		}

		logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

		return Flux.fromIterable(this.sessions.values())
			.flatMap(session -> session.sendNotification(method, params)
				.doOnError(
						e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
				.onErrorComplete())
			.then();
	}

	@Override
	public Mono<Void> notifyClient(String sessionId, String method, Object params) {
		return Mono.defer(() -> {
			McpStreamableServerSession session = this.sessions.get(sessionId);
			if (session == null) {
				logger.debug("Session {} not found", sessionId);
				return Mono.empty();
			}
			return session.sendNotification(method, params);
		});
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.defer(() -> {
			this.isClosing = true;
			return Flux.fromIterable(this.sessions.values())
				.doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions",
						this.sessions.size()))
				.flatMap(McpStreamableServerSession::closeGracefully)
				.then();
		}).then().doOnSuccess(v -> {
			this.sessions.clear();
			if (this.keepAliveScheduler != null) {
				this.keepAliveScheduler.shutdown();
			}
		});
	}

	public RouterFunction<?> getRouterFunction() {
		return this.routerFunction;
	}

	private Mono<ServerResponse> handleGet(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
		}

		try {
			Map<String, List<String>> headers = toHeaderMap(request.headers().asHttpHeaders());
			this.securityValidator.validateHeaders(headers);
		}
		catch (ServerTransportSecurityException e) {
			String errorMessage = e.getMessage();
			return ServerResponse.status(e.getStatusCode()).bodyValue(errorMessage != null ? errorMessage : "");
		}

		McpTransportContext transportContext = this.contextExtractor.extract(request);

		return Mono.defer(() -> {
			List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
			if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)) {
				return ServerResponse.badRequest().build();
			}

			if (request.headers().header(HttpHeaders.MCP_SESSION_ID).isEmpty()) {
				return ServerResponse.badRequest().build();
			}

			String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);

			McpStreamableServerSession session = this.sessions.get(sessionId);

			if (session == null) {
				return ServerResponse.notFound().build();
			}

			if (!request.headers().header(HttpHeaders.LAST_EVENT_ID).isEmpty()) {
				String lastId = request.headers().asHttpHeaders().getFirst(HttpHeaders.LAST_EVENT_ID);
				return ServerResponse.ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(session.replay(lastId)
						.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)),
							ServerSentEvent.class);
			}

			return ServerResponse.ok()
				.contentType(MediaType.TEXT_EVENT_STREAM)
				.body(Flux.<ServerSentEvent<?>>create(sink -> {
					WebFluxStreamableMcpSessionTransport sessionTransport = new WebFluxStreamableMcpSessionTransport(
							sink);
					McpStreamableServerSession.McpStreamableServerSessionStream listeningStream = session
						.listeningStream(sessionTransport);
					sink.onDispose(listeningStream::close);
				}).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)), ServerSentEvent.class);

		}).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
	}

	private Mono<ServerResponse> handlePost(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
		}

		try {
			Map<String, List<String>> headers = toHeaderMap(request.headers().asHttpHeaders());
			this.securityValidator.validateHeaders(headers);
		}
		catch (ServerTransportSecurityException e) {
			String errorMessage = e.getMessage();
			return ServerResponse.status(e.getStatusCode()).bodyValue(errorMessage != null ? errorMessage : "");
		}

		McpTransportContext transportContext = this.contextExtractor.extract(request);

		List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
		if (!(acceptHeaders.contains(MediaType.APPLICATION_JSON)
				&& acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM))) {
			return ServerResponse.badRequest().build();
		}

		return request.bodyToMono(String.class).<ServerResponse>flatMap(body -> {
			try {
				McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.jsonMapper, body);
				if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
						&& jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
					if (this.sessionFactory == null) {
						return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.bodyValue(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
								.message("Session factory not initialized")
								.build());
					}
					var typeReference = new TypeRef<McpSchema.InitializeRequest>() {
					};
					McpSchema.InitializeRequest initializeRequest = this.jsonMapper
						.convertValue(jsonrpcRequest.params(), typeReference);
					McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
						.startSession(initializeRequest);
					this.sessions.put(init.session().getId(), init.session());
					return init.initResult().map(initializeResult -> {
						McpSchema.JSONRPCResponse jsonrpcResponse = new McpSchema.JSONRPCResponse(
								McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initializeResult, null);
						try {
							return this.jsonMapper.writeValueAsString(jsonrpcResponse);
						}
						catch (IOException e) {
							logger.warn("Failed to serialize initResponse", e);
							throw Exceptions.propagate(e);
						}
					})
						.flatMap(initResult -> ServerResponse.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.header(HttpHeaders.MCP_SESSION_ID, init.session().getId())
							.bodyValue(initResult));
				}

				if (request.headers().header(HttpHeaders.MCP_SESSION_ID).isEmpty()) {
					return ServerResponse.badRequest()
						.bodyValue(McpError.builder(McpSchema.ErrorCodes.METHOD_NOT_FOUND)
							.message("Session ID missing")
							.build());
				}

				String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);
				McpStreamableServerSession session = this.sessions.get(sessionId);

				if (session == null) {
					return ServerResponse.status(HttpStatus.NOT_FOUND)
						.bodyValue(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
							.message("Session not found: " + sessionId)
							.build());
				}

				if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
					return session.accept(jsonrpcResponse).then(ServerResponse.accepted().build());
				}
				else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
					return session.accept(jsonrpcNotification).then(ServerResponse.accepted().build());
				}
				else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
					return ServerResponse.ok()
						.contentType(MediaType.TEXT_EVENT_STREAM)
						.body(Flux.<ServerSentEvent<?>>create(sink -> {
							WebFluxStreamableMcpSessionTransport st = new WebFluxStreamableMcpSessionTransport(sink);
							Mono<Void> stream = session.responseStream(jsonrpcRequest, st);
							Disposable streamSubscription = stream.onErrorComplete(err -> {
								sink.error(err);
								return true;
							}).contextWrite(sink.contextView()).subscribe();
							sink.onCancel(streamSubscription);
						}).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)),
								ServerSentEvent.class);
				}
				else {
					return ServerResponse.badRequest()
						.bodyValue(McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
							.message("Unknown message type")
							.build());
				}
			}
			catch (IllegalArgumentException | IOException e) {
				logger.error("Failed to deserialize message: {}", e.getMessage());
				return ServerResponse.badRequest()
					.bodyValue(McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
						.message("Invalid message format")
						.build());
			}
		})
			.switchIfEmpty(ServerResponse.badRequest().build())
			.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
	}

	private Mono<ServerResponse> handleDelete(ServerRequest request) {
		if (this.isClosing) {
			return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
		}

		try {
			Map<String, List<String>> headers = toHeaderMap(request.headers().asHttpHeaders());
			this.securityValidator.validateHeaders(headers);
		}
		catch (ServerTransportSecurityException e) {
			String errorMessage = e.getMessage();
			return ServerResponse.status(e.getStatusCode()).bodyValue(errorMessage != null ? errorMessage : "");
		}

		McpTransportContext transportContext = this.contextExtractor.extract(request);

		return Mono.defer(() -> {
			if (request.headers().header(HttpHeaders.MCP_SESSION_ID).isEmpty()) {
				return ServerResponse.badRequest().build();
			}

			if (this.disallowDelete) {
				return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
			}

			String sessionId = request.headers().asHttpHeaders().getFirst(HttpHeaders.MCP_SESSION_ID);

			McpStreamableServerSession session = this.sessions.get(sessionId);

			if (session == null) {
				return ServerResponse.notFound().build();
			}

			return session.delete().then(ServerResponse.ok().build());
		}).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
	}

	public static Builder builder() {
		return new Builder();
	}

	private class WebFluxStreamableMcpSessionTransport implements McpStreamableServerTransport {

		private final FluxSink<ServerSentEvent<?>> sink;

		WebFluxStreamableMcpSessionTransport(FluxSink<ServerSentEvent<?>> sink) {
			this.sink = sink;
		}

		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return this.sendMessage(message, null);
		}

		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
			return Mono.fromSupplier(() -> {
				try {
					return jsonMapper.writeValueAsString(message);
				}
				catch (IOException e) {
					throw Exceptions.propagate(e);
				}
			}).doOnNext(jsonText -> {
				var sseBuilder = ServerSentEvent.builder();
				if (messageId != null) {
					sseBuilder.id(messageId);
				}
				ServerSentEvent<Object> event = sseBuilder.event(MESSAGE_EVENT_TYPE).data(jsonText).build();
				this.sink.next(event);
			}).doOnError(e -> {
				Throwable exception = Exceptions.unwrap(e);
				this.sink.error(exception);
			}).then();
		}

		@Override
		public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
			return jsonMapper.convertValue(data, typeRef);
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(this.sink::complete);
		}

		@Override
		public void close() {
			this.sink.complete();
		}

	}

	public final static class Builder {

		private McpJsonMapper jsonMapper = McpJsonDefaults.getMapper();

		private String mcpEndpoint = "/mcp";

		private McpTransportContextExtractor<ServerRequest> contextExtractor = serverRequest -> McpTransportContext.EMPTY;

		private boolean disallowDelete;

		private Duration keepAliveInterval;

		private ServerTransportSecurityValidator securityValidator = ServerTransportSecurityValidator.NOOP;

		private Builder() {
		}

		public Builder jsonMapper(McpJsonMapper jsonMapper) {
			Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
			this.jsonMapper = jsonMapper;
			return this;
		}

		public Builder messageEndpoint(String messageEndpoint) {
			Assert.notNull(messageEndpoint, "Message endpoint must not be null");
			this.mcpEndpoint = messageEndpoint;
			return this;
		}

		public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
			Assert.notNull(contextExtractor, "contextExtractor must not be null");
			this.contextExtractor = contextExtractor;
			return this;
		}

		public Builder disallowDelete(boolean disallowDelete) {
			this.disallowDelete = disallowDelete;
			return this;
		}

		public Builder keepAliveInterval(Duration keepAliveInterval) {
			this.keepAliveInterval = keepAliveInterval;
			return this;
		}

		public Builder securityValidator(ServerTransportSecurityValidator securityValidator) {
			Assert.notNull(securityValidator, "Security validator must not be null");
			this.securityValidator = securityValidator;
			return this;
		}

		public WebFluxStreamableServerTransportProvider build() {
			Assert.notNull(this.mcpEndpoint, "Message endpoint must be set");
			return new WebFluxStreamableServerTransportProvider(this.jsonMapper, this.mcpEndpoint,
					this.contextExtractor, this.disallowDelete, this.keepAliveInterval, this.securityValidator);
		}

	}

}
