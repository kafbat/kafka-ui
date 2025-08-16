package io.kafbat.ui.service.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
public abstract class NgramFilter<T> {
  private final Analyzer analyzer;

  public NgramFilter(int minGram, int maxGram) {
    this.analyzer = new ShortWordNGramAnalyzer(minGram, maxGram, false);
  }

  protected abstract List<Tuple2<List<String>, T>> getItems();

  private static Map<String, List<String>> cache = new ConcurrentHashMap<>();

  public List<T> find(String search) {
    try {
      if (search == null || search.isBlank()) {
        return getItems().stream().map(Tuple2::getT2).toList();
      }
      List<SearchResult<T>> result = new ArrayList<>();
      List<String> queryTokens = tokenizeString(analyzer, search);
      Map<String, Integer> queryFreq = termFreq(queryTokens);

      for (Tuple2<List<String>, T> item : getItems()) {
        for (String field : item.getT1()) {
          List<String> itemTokens = tokenizeString(analyzer, field);
          HashSet<String> itemTokensSet = new HashSet<>(itemTokens);
          if (itemTokensSet.containsAll(queryTokens)) {
            double score = cosineSimilarity(queryFreq, itemTokens);
            result.add(new SearchResult<T>(item.getT2(), score));
          }
        }
      }
      result.sort((o1, o2) -> Double.compare(o2.score, o1.score));
      return result.stream().map(r -> r.item).toList();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private record SearchResult<T>(T item, double score) {
  }


  public static List<String> tokenizeString(Analyzer analyzer, String text) throws IOException {
    return cache.computeIfAbsent(text, (t) -> tokenizeStringSimple(analyzer, text));
  }

  @SneakyThrows
  public static List<String> tokenizeStringSimple(Analyzer analyzer, String text) {
    List<String> tokens = new ArrayList<>();
    try (TokenStream tokenStream = analyzer.tokenStream(null, text)) {
      CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
      tokenStream.reset();
      while (tokenStream.incrementToken()) {
        tokens.add(attr.toString());
      }
      tokenStream.end();
    }
    return tokens;
  }

  private static double cosineSimilarity(Map<String, Integer> queryFreq, List<String> itemTokens) {
    // Build frequency maps
    Map<String, Integer> terms = termFreq(itemTokens);

    double dot = 0.0;
    double mag1 = 0.0;
    double mag2 = 0.0;

    for (String term : terms.keySet()) {
      int f1 = queryFreq.getOrDefault(term, 0);
      int f2 = terms.getOrDefault(term, 0);
      dot += f1 * f2;
      mag1 += f1 * f1;
      mag2 += f2 * f2;
    }

    return (mag1 == 0 || mag2 == 0) ? 0.0 : dot / (Math.sqrt(mag1) * Math.sqrt(mag2));
  }

  private static Map<String, Integer> termFreq(List<String> tokens) {
    Map<String, Integer> freq = new HashMap<>();
    for (String token : tokens) {
      freq.put(token, freq.getOrDefault(token, 0) + 1);
    }
    return freq;
  }
}
