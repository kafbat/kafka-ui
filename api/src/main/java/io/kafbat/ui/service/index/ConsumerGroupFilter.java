package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalConsumerGroup;
import java.util.List;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class ConsumerGroupFilter extends NgramFilter<InternalConsumerGroup> {
  private final List<Tuple2<String, InternalConsumerGroup>> groups;

  public ConsumerGroupFilter(List<InternalConsumerGroup> groups) {
    this.groups = groups.stream().map(g -> Tuples.of(g.getGroupId(), g)).toList();
  }

  @Override
  protected List<Tuple2<String, InternalConsumerGroup>> getItems() {
    return this.groups;
  }
}
