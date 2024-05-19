package io.kafbat.ui.screens.ksqldb.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Stream {

  private String name, topicName, valueFormat, partitions;
}
