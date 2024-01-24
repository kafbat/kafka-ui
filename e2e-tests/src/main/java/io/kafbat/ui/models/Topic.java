package io.kafbat.ui.models;

import io.kafbat.ui.pages.topics.enums.CleanupPolicyValue;
import io.kafbat.ui.pages.topics.enums.CustomParameterType;
import io.kafbat.ui.pages.topics.enums.MaxSizeOnDisk;
import io.kafbat.ui.pages.topics.enums.TimeToRetain;
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
}
