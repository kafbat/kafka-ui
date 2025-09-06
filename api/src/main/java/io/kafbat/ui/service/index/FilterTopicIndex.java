package io.kafbat.ui.service.index;

import static org.apache.commons.lang3.Strings.CI;

import io.kafbat.ui.model.InternalTopic;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FilterTopicIndex implements TopicsIndex {
  private List<InternalTopic> topics;

  public FilterTopicIndex(List<InternalTopic> topics) {
    this.topics = topics;
  }

  @Override
  public List<InternalTopic> find(String search, Boolean showInternal, String sort, Integer count) {
    if (search == null || search.isBlank()) {
      return new ArrayList<>(this.topics);
    }
    Stream<InternalTopic> stream = topics.stream().filter(topic -> !topic.isInternal()
            || showInternal != null && showInternal)
        .filter(
            topic -> search == null || CI.contains(topic.getName(), search)
        );

    return stream.toList();
  }

  @Override
  public void close() throws Exception {

  }
}
