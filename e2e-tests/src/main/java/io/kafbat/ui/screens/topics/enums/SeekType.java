package io.kafbat.ui.screens.topics.enums;

import lombok.Getter;

@Getter
public enum SeekType {

  OLDEST("Oldest"),
  NEWEST("Newest"),
  LIVE("Live"),
  FROM_OFFSET("From offset"),
  TO_OFFSET("To offset"),
  SINCE_TIME("Since time"),
  TO_TIME("To time");

  private final String value;

  SeekType(String value) {
    this.value = value;
  }
}
