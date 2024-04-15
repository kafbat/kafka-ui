package io.kafbat.ui.models;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

import io.kafbat.ui.screens.topics.enums.CleanupPolicyValue;
import io.kafbat.ui.screens.topics.enums.CustomParameterType;
import io.kafbat.ui.screens.topics.enums.MaxSizeOnDisk;
import io.kafbat.ui.screens.topics.enums.TimeToRetain;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Topic {

  private String name, timeToRetainData, maxMessageBytes, messageKey, messageValue, customParameterValue;
  private int numberOfPartitions;
  private CustomParameterType customParameterType;
  private CleanupPolicyValue cleanupPolicyValue;
  private MaxSizeOnDisk maxSizeOnDisk;
  private TimeToRetain timeToRetain;

  public static Topic createTopic() {
    return new Topic()
        .setName("aqa_topic_" + randomAlphabetic(5))
        .setNumberOfPartitions(1)
        .setMessageKey(randomAlphabetic(5))
        .setMessageValue(randomAlphabetic(10));
  }
}
