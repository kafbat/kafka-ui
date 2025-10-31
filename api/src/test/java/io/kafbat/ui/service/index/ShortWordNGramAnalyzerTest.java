package io.kafbat.ui.service.index;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ShortWordNGramAnalyzerTest {

  @ParameterizedTest
  @MethodSource("provider")
  public void testOffsets(String name, List<String> parts) {
    ShortWordNGramAnalyzer analyzer = new ShortWordNGramAnalyzer(2, 4);
    List<String> strings = NgramFilter.tokenizeString(analyzer, name);
    assertThat(strings).isEqualTo(parts);
  }

  public static Stream<Arguments> provider() {
    return Stream.of(
        Arguments.of("hello.world.text", List.of(
            "he", "hel", "hell", "el", "ell", "ello", "ll", "llo", "lo", "hello",
            "wo", "wor", "worl", "or", "orl", "orld", "rl", "rld", "ld", "world", "te",
            "tex", "text", "ex", "ext", "xt"
        )),
        Arguments.of("helloWorldText", List.of(
            "he", "hel", "hell", "el", "ell", "ello", "ll", "llo", "lo", "hello",
            "wo", "wor", "worl", "or", "orl", "orld", "rl", "rld", "ld", "world", "te",
            "tex", "text", "ex", "ext", "xt"
        )),
        Arguments.of("hello:world:text", List.of(
            "he", "hel", "hell", "el", "ell", "ello", "ll", "llo", "lo", "hello",
            "wo", "wor", "worl", "or", "orl", "orld", "rl", "rld", "ld", "world", "te",
            "tex", "text", "ex", "ext", "xt"
        ))
    );
  }
}
