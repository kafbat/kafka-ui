package io.kafbat.ui.service.index.lucene;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

class ShortWordAnalyzerTest {

  @ParameterizedTest
  @MethodSource("provider")
  public void testOffsets(String name, List<Tuple3<String, Integer, Integer>> parts) throws Exception {
    ShortWordAnalyzer analyzer = new ShortWordAnalyzer();

    TokenStream ts = analyzer.tokenStream("content", new StringReader(name));
    CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
    OffsetAttribute offAtt = ts.addAttribute(OffsetAttribute.class);
    ts.reset();

    List<Tuple3<String, Integer, Integer>> calculated = new ArrayList<>();
    while (ts.incrementToken()) {
      calculated.add(Tuples.of(termAtt.toString(), offAtt.startOffset(), offAtt.endOffset()));
    }
    ts.end();
    ts.close();

    assertThat(calculated).isEqualTo(parts);
  }

  public static Stream<Arguments> provider() {
    return Stream.of(
        Arguments.of("hello.world.text", List.of(
            Tuples.of("hello.world.text", 0, 16),
            Tuples.of("hello", 0, 5),
            Tuples.of("world", 6, 11),
            Tuples.of("text", 12, 16)
        )),
        Arguments.of("helloWorldText", List.of(
            Tuples.of("helloworldtext", 0, 14),
            Tuples.of("hello", 0, 5),
            Tuples.of("world", 5, 10),
            Tuples.of("text", 10, 14)
        )),
        Arguments.of("hello:world:text", List.of(
            Tuples.of("hello:world:text", 0, 16),
            Tuples.of("hello", 0, 5),
            Tuples.of("world", 6, 11),
            Tuples.of("text", 12, 16)
        ))
    );
  }
}
