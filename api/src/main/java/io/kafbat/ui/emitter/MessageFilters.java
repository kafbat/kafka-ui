package io.kafbat.ui.emitter;

import static java.util.Collections.emptyMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelOptions;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import io.kafbat.ui.exception.CelException;
import io.kafbat.ui.model.MessageFilterTypeDTO;
import io.kafbat.ui.model.TopicMessageDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class MessageFilters {
  private static final CelCompiler CEL_COMPILER = createCompiler();
  private static final CelRuntime CEL_RUNTIME = CelRuntimeFactory.standardCelRuntimeBuilder().build();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Predicate<TopicMessageDTO> createMsgFilter(String query, MessageFilterTypeDTO type) {
    return switch (type) {
      case STRING_CONTAINS -> containsStringFilter(query);
      case CEL_SCRIPT -> celScriptFilter(query);
    };
  }

  static Predicate<TopicMessageDTO> containsStringFilter(String string) {
    return msg -> StringUtils.contains(msg.getKey(), string)
        || StringUtils.contains(msg.getContent(), string);
  }

  static Predicate<TopicMessageDTO> celScriptFilter(String script) {
    CelValidationResult celValidationResult = CEL_COMPILER.compile(script);
    if (celValidationResult.hasError()) {
      throw new CelException(script, celValidationResult.getErrorString());
    }

    try {
      CelAbstractSyntaxTree ast = celValidationResult.getAst();
      CelRuntime.Program program = CEL_RUNTIME.createProgram(ast);

      return createPredicate(script, program);
    } catch (CelValidationException | CelEvaluationException e) {
      throw new CelException(script, e);
    }
  }

  private static Predicate<TopicMessageDTO> createPredicate(String originalScript, CelRuntime.Program program) {
    return topicMessage -> {
      Object programResult;
      try {
        programResult = program.eval(recordToArgs(topicMessage));
      } catch (CelEvaluationException e) {
        throw new CelException(originalScript, e);
      }

      if (programResult instanceof Boolean isMessageMatched) {
        return isMessageMatched;
      }

      throw new CelException(
          originalScript,
          "Unexpected script result, boolean should be returned instead. Script output: %s".formatted(programResult)
      );
    };
  }

  private static Map<String, Object> recordToArgs(TopicMessageDTO topicMessage) {
    Map<String, Object> args = new HashMap<>();

    args.put("partition", topicMessage.getPartition());
    args.put("offset", topicMessage.getOffset());

    if (topicMessage.getTimestamp() != null) {
      args.put("timestampMs", topicMessage.getTimestamp().toInstant().toEpochMilli());
    }

    args.put("keyAsText", Objects.requireNonNullElse(topicMessage.getKey(), ""));
    args.put("valueAsText", Objects.requireNonNullElse(topicMessage.getContent(), ""));

    if (topicMessage.getKey() != null) {
      args.put("key", parseToJsonOrReturnAsIs(topicMessage.getKey()));
    } else {
      args.put("key", emptyMap());
    }

    if (topicMessage.getContent() != null) {
      args.put("value", parseToJsonOrReturnAsIs(topicMessage.getContent()));
    } else {
      args.put("value", emptyMap());
    }

    args.put("headers", Objects.requireNonNullElse(topicMessage.getHeaders(), emptyMap()));

    return args;
  }

  private static CelCompiler createCompiler() {
    CelCompilerBuilder celCompilerBuilder = CelCompilerFactory.standardCelCompilerBuilder()
        .setOptions(CelOptions.DEFAULT)
        .setStandardMacros(CelStandardMacro.STANDARD_MACROS);

    celCompilerBuilder.addVar("partition", SimpleType.INT);
    celCompilerBuilder.addVar("offset", SimpleType.INT);
    celCompilerBuilder.addVar("timestampMs", SimpleType.INT);
    celCompilerBuilder.addVar("keyAsText", SimpleType.STRING);
    celCompilerBuilder.addVar("valueAsText", SimpleType.STRING);
    celCompilerBuilder.addVar("headers", MapType.create(SimpleType.STRING, SimpleType.STRING));
    celCompilerBuilder.addVar("key", SimpleType.DYN);
    celCompilerBuilder.addVar("value", SimpleType.DYN);

    return celCompilerBuilder
        .setResultType(SimpleType.BOOL)
        .build();
  }

  @Nullable
  private static Object parseToJsonOrReturnAsIs(@Nullable String str) {
    if (str == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(str, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      return str;
    }
  }
}
