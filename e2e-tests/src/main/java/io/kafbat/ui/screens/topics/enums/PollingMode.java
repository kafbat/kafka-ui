package io.kafbat.ui.screens.topics.enums;

import lombok.Getter;

@Getter
public enum PollingMode {

  OLDEST("Oldest"),
  NEWEST("Newest"),
  LIVE("Live"),
  FROM_OFFSET("From offset"),
  TO_OFFSET("To offset"),
  SINCE_TIME("Since time"),
  TO_TIME("To time");

  private final String value;

  PollingMode(String value) {
    this.value = value;
  }
}
