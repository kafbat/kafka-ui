package io.kafbat.ui.emitter;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.Strings.CS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.NullValue;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelOptions;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelType;
import dev.cel.common.types.CelTypeProvider;
import dev.cel.common.types.MapType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.extensions.CelExtensions;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import io.kafbat.ui.exception.CelException;
import io.kafbat.ui.model.TopicMessageDTO;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class MessageFilters {

  private static final String CEL_RECORD_VAR_NAME = "record";
  private static final String CEL_RECORD_TYPE_NAME = TopicMessageDTO.class.getSimpleName();

  private static final CelCompiler CEL_COMPILER = createCompiler();
  private static final CelRuntime CEL_RUNTIME = createRuntime();
  private static final Object CELL_NULL_VALUE = NullValue.NULL_VALUE;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Predicate<TopicMessageDTO> noop() {
    return e -> true;
  }

  public static Predicate<TopicMessageDTO> containsStringFilter(String string) {
    return msg -> CS.contains(msg.getKey(), string)
        || CS.contains(msg.getValue(), string) || headersContains(msg, string);
  }

  private static boolean headersContains(TopicMessageDTO msg, String searchString) {
    final var headers = msg.getHeaders();

    if (headers == null) {
      return false;
    }

    for (final var entry : headers.entrySet()) {
      if (CS.contains(entry.getKey(), searchString) || CS.contains(entry.getValue(), searchString)) {
        return true;
      }
    }

    return false;
  }

  public static Predicate<TopicMessageDTO> celScriptFilter(String script) {
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

  private static Map<String, Map<String, Object>> recordToArgs(TopicMessageDTO topicMessage) {
    Map<String, Object> args = new HashMap<>();

    args.put("partition", topicMessage.getPartition());
    args.put("offset", topicMessage.getOffset());

    if (topicMessage.getTimestamp() != null) {
      args.put("timestampMs", topicMessage.getTimestamp().toInstant().toEpochMilli());
    }

    if (topicMessage.getKey() != null) {
      args.put("key", parseToJsonOrReturnAsIs(topicMessage.getKey()));
      args.put("keyAsText", topicMessage.getKey());
    }

    if (topicMessage.getValue() != null) {
      args.put("value", parseToJsonOrReturnAsIs(topicMessage.getValue()));
      args.put("valueAsText", topicMessage.getValue());
    }

    args.put("headers", Objects.requireNonNullElse(topicMessage.getHeaders(), emptyMap()));

    return Map.of("record", args);
  }

  private static CelCompiler createCompiler() {
    Map<String, CelType> fields = Map.of(
        "partition", SimpleType.INT,
        "offset", SimpleType.INT,
        "timestampMs", SimpleType.INT,
        "keyAsText", SimpleType.STRING,
        "valueAsText", SimpleType.STRING,
        "headers", MapType.create(SimpleType.STRING, SimpleType.STRING),
        "key", SimpleType.DYN,
        "value", SimpleType.DYN
    );

    ImmutableSet<String> names = ImmutableSet
        .<String>builder()
        .addAll(fields.keySet())
        .build();

    StructType recordType = StructType.create(
        CEL_RECORD_TYPE_NAME,
        names,
        fieldName -> Optional.ofNullable(fields.get(fieldName))
    );

    return CelCompilerFactory.standardCelCompilerBuilder()
        .setOptions(CelOptions.DEFAULT)
        .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
        .addLibraries(CelExtensions.strings(), CelExtensions.encoders())
        .addVar(CEL_RECORD_VAR_NAME, recordType)
        .setResultType(SimpleType.BOOL)
        .setTypeProvider(new CelTypeProvider() {
          @Override
          public ImmutableCollection<CelType> types() {
            return ImmutableSet.of(recordType);
          }

          @Override
          public Optional<CelType> findType(String typeName) {
            return CEL_RECORD_TYPE_NAME.equals(typeName) ? Optional.of(recordType) : Optional.empty();
          }
        })
        .build();
  }

  private static CelRuntime createRuntime() {
    return CelRuntimeFactory.standardCelRuntimeBuilder()
        .addLibraries(CelExtensions.strings(), CelExtensions.encoders())
        .build();
  }

  @Nullable
  private static Object parseToJsonOrReturnAsIs(@Nullable String str) {
    if (str == null) {
      return null;
    }

    try {
      //@formatter:off
      var map = OBJECT_MAPPER.readValue(str, new TypeReference<Map<String, Object>>() {});
      //@formatter:on
      return replaceCelNulls(map);
    } catch (JsonProcessingException e) {
      return str;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> replaceCelNulls(Map<String, Object> map) {
    var result = new LinkedHashMap<String, Object>();

    for (var entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value == null) {
        result.put(key, CELL_NULL_VALUE);
      } else if (value instanceof Map<?, ?>) {
        var inner = (Map<String, Object>) value;
        result.put(key, replaceCelNulls(inner));
      } else {
        result.put(key, value);
      }
    }

    return result;
  }

}
