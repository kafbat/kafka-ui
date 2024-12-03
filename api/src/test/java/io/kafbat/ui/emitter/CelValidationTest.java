package io.kafbat.ui.emitter;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelOptions;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelType;
import dev.cel.common.types.CelTypeProvider;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.extensions.CelExtensions;
import dev.cel.parser.CelStandardMacro;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CelValidationTest {
    private final Map<String, CelType> fields = Map.of(
        "value", SimpleType.DYN
    );

  private final ImmutableSet<String> names = ImmutableSet
        .<String>builder()
        .addAll(fields.keySet())
        .build();

  private final StructType recordType = StructType.create(
        "MessageDTO",
        names,
        fieldName -> Optional.ofNullable(fields.get(fieldName))
    );

  private final CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder()
        .setOptions(CelOptions.DEFAULT)
        .setStandardMacros(CelStandardMacro.STANDARD_MACROS)
        .addLibraries(CelExtensions.strings(), CelExtensions.encoders())
        .addVar("record", recordType)
        .setResultType(SimpleType.BOOL)
        .setTypeProvider(new CelTypeProvider() {
          @Override
          public ImmutableCollection<CelType> types() {
            return ImmutableSet.of(recordType);
          }

          @Override
          public Optional<CelType> findType(String typeName) {
            return "MessageDTO".equals(typeName) ? Optional.of(recordType) : Optional.empty();
          }
        })
        .build();

  private final CelRuntime celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();

  @ParameterizedTest
  @CsvSource({
      "record.value.lastname == null, true",
      "record.value.firstname == 'Paul', true",
      "size(record.value.firstname) == 4, true",
      "record.value.age == 24, true",
      "record.value.age >= 24, true",
      "record.value.age > 24, true",
      "has(record.value.age), true",
      "has(record.value.ages), false",
      "record.value.age < 50, true",
      "record.value.age < 24, true"
  })
  void expressionTest(String expression, boolean expected) throws CelValidationException, CelEvaluationException {
    Map<String, Object> objectKeys = new HashMap<>();
    objectKeys.put("lastname", null);
    objectKeys.put("firstname", "Paul");
    objectKeys.put("age", 24);
    Map<String, Object> valueKeys = new HashMap<>();
    valueKeys.put("value", objectKeys);
    Map<String, Map<String, Object>> recordKeys = new HashMap<>();
    recordKeys.put("record", valueKeys);

    CelValidationResult celValidationResult = celCompiler.compile(expression);
    CelAbstractSyntaxTree ast = celValidationResult.getAst();
    CelRuntime.Program program = celRuntime.createProgram(ast);
    var programResult = program.eval(recordKeys);
    assertThat(programResult)
        .describedAs("The result of the assertion was incorrect")
        .isNotNull()
        .isEqualTo(expected)
    ;
  }
}
