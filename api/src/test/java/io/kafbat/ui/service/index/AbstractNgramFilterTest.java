package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

abstract class AbstractNgramFilterTest<T> {
  private final ClustersProperties.NgramProperties ngramProperties = new ClustersProperties.NgramProperties();


  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testFind(boolean enabled) {
    List<T> items = items();

    NgramFilter<T> filter = buildFilter(items, enabled, ngramProperties);
    List<T> resultNoSearch = filter.find(null);
    assertThat(resultNoSearch).isNotEmpty().containsExactlyInAnyOrderElementsOf(items);

    List<T> resultWithCompare = filter.find(null, comparator());
    assertThat(resultWithCompare).isNotEmpty().containsExactlyInAnyOrderElementsOf(items);

    Map.Entry<String, T> example = example(items);
    List<T> resultNoCompare = filter.find(example.getKey());
    assertThat(resultNoCompare).isNotEmpty().contains(example.getValue());
  }

  @Test
  public void testOrder() {
    List<T> items = sortedItems();
    NgramFilter<T> filter = buildFilter(items, true, ngramProperties);
    List<T> result = filter.find(sortedExample(items));
    assertThat(result).isEqualTo(sortedResult(items));
  }



  protected abstract NgramFilter<T> buildFilter(List<T> items,
                                                boolean enabled,
                                                ClustersProperties.NgramProperties ngramProperties);

  protected abstract List<T> items();

  protected abstract Comparator<T> comparator();

  protected abstract Map.Entry<String, T> example(List<T> items);

  protected abstract List<T> sortedItems();

  protected abstract String sortedExample(List<T> items);

  protected abstract List<T> sortedResult(List<T> items);
}
