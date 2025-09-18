package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class SchemasFilterTest extends AbstractNgramFilterTest<String> {

  @Override
  protected NgramFilter<String> buildFilter(List<String> items, boolean enabled,
                                            ClustersProperties.NgramProperties ngramProperties) {
    return new SchemasFilter(items, enabled, ngramProperties);
  }

  @Override
  protected List<String> items() {
    return IntStream.range(0, 100).mapToObj(i -> "schema-" + i).toList();
  }

  @Override
  protected Comparator<String> comparator() {
    return String::compareTo;
  }

  @Override
  protected Map.Entry<String, String> example(List<String> items) {
    String item = items.getFirst();
    return Map.entry(item, item);
  }
}
