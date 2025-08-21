package io.kafbat.ui.service.index;

import static org.apache.commons.lang3.Strings.CI;

import java.util.Collection;
import java.util.List;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class SchemasFilter extends NgramFilter<String> {
  private final List<Tuple2<List<String>, String>> subjects;
  private final boolean fts;

  public SchemasFilter(Collection<String> subjects) {
    this(subjects, 1, 4, true);
  }

  public SchemasFilter(Collection<String> subjects, int minNGram, int maxNGram, boolean fts) {
    super(minNGram, maxNGram);
    this.subjects = subjects.stream().map(g -> Tuples.of(List.of(g), g)).toList();
    this.fts = fts;
  }

  @Override
  protected List<Tuple2<List<String>, String>> getItems() {
    return this.subjects;
  }

  @Override
  public List<String> find(String search) {
    if (fts) {
      return super.find(search);
    } else {
      return this.subjects
          .stream()
          .map(Tuple2::getT2)
          .filter(subj -> search == null || CI.contains(subj, search))
          .sorted().toList();
    }
  }
}
