package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.apache.kafka.clients.admin.ConsumerGroupListing;

class ConsumerGroupFilterTest extends AbstractNgramFilterTest<ConsumerGroupListing> {

  @Override
  protected NgramFilter<ConsumerGroupListing> buildFilter(List<ConsumerGroupListing> items,
                                                          boolean enabled,
                                                          ClustersProperties.NgramProperties ngramProperties) {
    return new ConsumerGroupFilter(items, enabled, ngramProperties);
  }

  @Override
  protected List<ConsumerGroupListing> items() {
    return IntStream.range(0, 100).mapToObj(i ->
        new ConsumerGroupListing("resource-" + i, true)
    ).toList();
  }

  @Override
  protected Comparator<ConsumerGroupListing> comparator() {
    return Comparator.comparing(ConsumerGroupListing::groupId);
  }

  @Override
  protected Map.Entry<String, ConsumerGroupListing> example(List<ConsumerGroupListing> items) {
    ConsumerGroupListing first = items.getFirst();
    return Map.entry(first.groupId(), first);
  }
}
