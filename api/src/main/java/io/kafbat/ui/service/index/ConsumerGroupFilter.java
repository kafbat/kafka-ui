package io.kafbat.ui.service.index;

import java.util.Collection;
import java.util.List;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class ConsumerGroupFilter extends NgramFilter<ConsumerGroupListing> {
  private final List<Tuple2<List<String>, ConsumerGroupListing>> groups;

  public ConsumerGroupFilter(Collection<ConsumerGroupListing> groups) {
    this(groups, 1, 4);
  }

  public ConsumerGroupFilter(Collection<ConsumerGroupListing> groups, int minNGram, int maxNGram) {
    super(minNGram, maxNGram);
    this.groups = groups.stream().map(g -> Tuples.of(List.of(g.groupId()), g)).toList();
  }

  @Override
  protected List<Tuple2<List<String>, ConsumerGroupListing>> getItems() {
    return this.groups;
  }
}
