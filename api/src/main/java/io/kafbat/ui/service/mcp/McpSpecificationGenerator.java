package io.kafbat.ui.service.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpSpecificationGenerator {
  private final SchemaGenerator schemaGenerator;
  private final ObjectMapper objectMapper;

  public List<AsyncToolSpecification> convertTool(McpTool controller) {
    List<AsyncToolSpecification> result = new ArrayList<>();
    Class<?> targetClass = AopUtils.getTargetClass(controller);
    for (Method method : targetClass.getMethods()) {
      Deprecated deprecated = AnnotationUtils.findAnnotation(method, Deprecated.class);
      if (deprecated == null) {
        Operation annotation = AnnotationUtils.findAnnotation(method, Operation.class);
        if (annotation != null) {
          result.add(this.convertOperation(method, annotation, controller));
        }
      }
    }
    return result;
  }

  private AsyncToolSpecification convertOperation(Method method, Operation annotation, McpTool instance) {
    String name = annotation.operationId();
    String description = annotation.description().isEmpty() ? name : annotation.description();
    return new AsyncToolSpecification(
        McpSchema.Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(operationSchema(method, instance))
            .build(),
        methodCall(method, instance)
    );
  }

  @SuppressWarnings("unchecked")
  private BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<CallToolResult>>
      methodCall(Method method, Object instance) {

    return (ex, args) -> Mono.deferContextual(ctx -> {
      try {
        ServerWebExchange serverWebExchange = ctx.get(ServerWebExchange.class);
        Mono<Object> result = (Mono<Object>) method.invoke(
            instance,
            toParams(args, method.getParameters(), ex, serverWebExchange)
        );
        return result.flatMap(this::toCallResult)
              .onErrorResume((e) -> Mono.just(this.toErrorResult(e)));
      } catch (IllegalAccessException | InvocationTargetException e) {
        log.warn("Error invoking method {}: {}", method.getName(), e.getMessage(), e);
        return Mono.just(this.toErrorResult(e));
      }
    });
  }

  private Mono<CallToolResult> toCallResult(Object result) {
    return switch (result) {
      case Mono<?> mono -> mono.map(this::callToolResult);
      case Flux<?> flux -> flux.collectList().map(this::callToolResult);
      case ResponseEntity<?> response -> reponseToCallResult(response);
      case null, default -> Mono.just(this.callToolResult(result));
    };
  }

  private Mono<CallToolResult> reponseToCallResult(ResponseEntity<?> response) {
    HttpStatusCode statusCode = response.getStatusCode();
    if (statusCode.is2xxSuccessful() || statusCode.is1xxInformational()) {
      return Mono.just(this.callToolResult(response.getBody()));
    } else {
      try {
        return Mono.just(toErrorResult(objectMapper.writeValueAsString(response.getBody())));
      } catch (JsonProcessingException e) {
        return Mono.just(toErrorResult(e));
      }
    }
  }

  private CallToolResult callToolResult(Object result) {
    try {
      return new CallToolResult(
          List.of(new McpSchema.TextContent(objectMapper.writeValueAsString(result))),
          false
      );
    } catch (Exception e) {
      return toErrorResult(e);
    }
  }

  protected CallToolResult toErrorResult(String body) {
    return new CallToolResult(
        List.of(new McpSchema.TextContent(body)),
        true
    );
  }

  protected CallToolResult toErrorResult(Throwable e) {
    log.warn("Error responded to MCP Client: {}", e.getMessage(), e);
    return new CallToolResult(
        List.of(new McpSchema.TextContent(e.getMessage())),
        true
    );
  }

  private Object[] toParams(
      Map<String, Object> mcpArgs,
      Parameter[] parameters,
      McpAsyncServerExchange ex,
      ServerWebExchange serverWebExchange
  ) {
    Object[] values = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      if (parameter.getType().equals(ServerWebExchange.class)) {
        values[i] = serverWebExchange;
      } else if (parameter.getType().equals(McpAsyncServerExchange.class)) {
        values[i] = ex;
      } else {
        Object arg = mcpArgs.get(parameter.getName());
        if (arg != null) {
          Class<?> parameterType = parameter.getType();
          boolean mono = false;

          if (parameterType.isAssignableFrom(Mono.class)) {
            ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            parameterType = (Class<?>) actualTypeArguments[0];
            mono = true;
          }

          if (parameterType.isAssignableFrom(arg.getClass())) {
            values[i] = mono ? Mono.just(arg) : arg;
          } else if (Map.class.isAssignableFrom(arg.getClass())) {
            try {
              Object obj = objectMapper.convertValue(arg, parameterType);
              values[i] = mono ? Mono.just(obj) : obj;
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }

    return values;
  }

  private JsonSchema operationSchema(Method method, McpTool instance) {
    Method annotatedMethod = findAnnotatedMethod(method, instance);

    Map<String, Object> parametersSchemas = new HashMap<>();
    List<String> required = new ArrayList<>();
    Parameter[] annotatedParameters = annotatedMethod.getParameters();
    Parameter[] methodParameters = method.getParameters();
    for (int i = 0; i < methodParameters.length; i++) {
      Parameter methodParameter = methodParameters[i];
      Parameter annotatedParameter = annotatedParameters[i];

      io.swagger.v3.oas.annotations.Parameter parameterAnnotation =
          annotatedParameter.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
      if (!parameterAnnotation.hidden()) {
        if (parameterAnnotation.required()) {
          required.add(methodParameter.getName());
        }
        parametersSchemas.put(
            methodParameter.getName(),
            getTypeSchema(methodParameter)
        );
      }
    }
    return new JsonSchema(
        "object", parametersSchemas, required,
        false,
        null, null
    );
  }


  private Method findAnnotatedMethod(Method method, McpTool instance) {
    Class<?> declaringClass = AopUtils.getTargetClass(instance);
    for (Class<?> iface : declaringClass.getInterfaces()) {
      try {
        Method interfaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
        if (interfaceMethod.isAnnotationPresent(Operation.class)) {
          return interfaceMethod;
        }
      } catch (NoSuchMethodException ignored) {
        // Skip if no method in interface
      }
    }
    throw new RuntimeException(new NoSuchMethodException(method.getName()));
  }

  private Object getTypeSchema(Parameter parameter) {
    Class<?> type = parameter.getType();
    if (type.isAssignableFrom(Mono.class)) {
      ParameterizedType paramType = (ParameterizedType) parameter.getParameterizedType();
      Type[] actualTypeArguments = paramType.getActualTypeArguments();
      Type actualTypeArgument = actualTypeArguments[0];
      if (actualTypeArgument instanceof Class<?> clz) {
        return getTypeSchema(clz);
      } else if (actualTypeArgument instanceof ParameterizedType prm) {
        return getTypeSchema((Class<?>) prm.getRawType());
      } else {
        throw new UnsupportedOperationException(
            "TypeVariable, WildcardType, and GenericArrayType do not supported now"
        );
      }
    } else {
      return getTypeSchema(type);
    }
  }

  private Object getTypeSchema(Class<?> type) {
    return switch (type) {
      case Class<?> clz when clz.isAssignableFrom(String.class) -> Map.of("type", "string");
      case Class<?> clz when clz.isAssignableFrom(Integer.class) -> Map.of("type", "integer");
      case Class<?> clz when clz.isAssignableFrom(Long.class) -> Map.of("type", "integer");
      case Class<?> clz when clz.isAssignableFrom(Double.class) -> Map.of("type", "number");
      case Class<?> clz when clz.isAssignableFrom(Float.class) -> Map.of("type", "number");
      case Class<?> clz when clz.isAssignableFrom(Boolean.class) -> Map.of("type", "boolean");
      default -> schemaGenerator.generateSchema(type);
    };
  }


}
