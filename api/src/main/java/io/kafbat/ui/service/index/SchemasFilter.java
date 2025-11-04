package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Collection;
import java.util.List;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class SchemasFilter extends NgramFilter<String> {
  private final List<Tuple2<List<String>, String>> subjects;

  public SchemasFilter(Collection<String> subjects, boolean enabled, ClustersProperties.NgramProperties properties) {
    super(properties, enabled);
    this.subjects = subjects.stream().map(g -> Tuples.of(List.of(g), g)).toList();
  }

  @Override
  public List<String> find(String search) {
    return super.find(search, null);
  }

  @Override
  protected List<Tuple2<List<String>, String>> getItems() {
    return this.subjects;
  }
}
