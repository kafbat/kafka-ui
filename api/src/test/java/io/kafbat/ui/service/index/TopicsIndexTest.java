package io.kafbat.ui.service.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.kafbat.ui.model.InternalTopic;
import java.util.List;
import org.junit.jupiter.api.Test;

class TopicsIndexTest {
  @Test
  void testFindTopicsByName() throws Exception {
    List<InternalTopic> topics =
        List.of("test", "test-1", "test-2", "test-3", "test-4", "test-5", "test-6", "test-7", "test-8").stream()
            .map(s -> InternalTopic.builder().name(s).build()).toList();

    try(TopicsIndex index = new TopicsIndex(topics)) {
//      List<String> resultAll = index.find("test", null, topics.size());
//      assertThat(resultAll.size()).isEqualTo(topics.size());
//
//      List<String> resultOne = index.find("8", null, topics.size());
//      assertThat(resultOne.size()).isEqualTo(1);
//
//      List<String> resultEmpty = index.find("9", null, topics.size());
//      assertThat(resultEmpty.size()).isEqualTo(0);
//
//      List<String> resultAllFuzzy = index.find("tst", null, topics.size());
//      assertThat(resultAllFuzzy.size()).isEqualTo(topics.size());

      List<String> resultOneFuzzy = index.find("tst1", null, topics.size());
      assertThat(resultOneFuzzy.size()).isEqualTo(1);

    }
  }

}
