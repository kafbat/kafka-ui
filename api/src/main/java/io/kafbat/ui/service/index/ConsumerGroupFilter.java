package io.kafbat.ui.service.index;

import io.kafbat.ui.config.ClustersProperties;
import java.util.Collection;
import java.util.List;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class ConsumerGroupFilter extends NgramFilter<ConsumerGroupListing> {
  private final List<Tuple2<List<String>, ConsumerGroupListing>> groups;

  public ConsumerGroupFilter(Collection<ConsumerGroupListing> groups) {
    this(groups, true, new ClustersProperties.NgramProperties(1, 4, true));
  }

  public ConsumerGroupFilter(
      Collection<ConsumerGroupListing> groups,
      boolean enabled,
      ClustersProperties.NgramProperties properties) {
    super(properties, enabled);
    this.groups = groups.stream().map(g -> Tuples.of(List.of(g.groupId()), g)).toList();
  }

  @Override
  protected List<Tuple2<List<String>, ConsumerGroupListing>> getItems() {
    return this.groups;
  }
}
