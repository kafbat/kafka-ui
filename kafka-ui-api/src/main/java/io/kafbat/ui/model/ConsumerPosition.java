package io.kafbat.ui.model;

import io.kafbat.ui.model.SeekTypeDTO;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;
import org.apache.kafka.common.TopicPartition;

@Value
public class ConsumerPosition {
  SeekTypeDTO seekType;
  String topic;
  @Nullable
  Map<TopicPartition, Long> seekTo; // null if positioning should apply to all tps
}
