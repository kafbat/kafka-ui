package io.kafbat.ui.model.state;

import java.util.Map;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.common.TopicPartition;

public record ConsumerGroupState(
    String group,
    ConsumerGroupDescription description,
    Map<TopicPartition, Long> committedOffsets) {
}
