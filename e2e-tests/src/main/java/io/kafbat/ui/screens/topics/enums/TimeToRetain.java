package io.kafbat.ui.screens.topics.enums;

import lombok.Getter;

@Getter
public enum TimeToRetain {

  BTN_1_HOUR("1 hour", "3600000"),
  BTN_3_HOURS("3 hours", "10800000"),
  BTN_6_HOURS("6 hours", "21600000"),
  BTN_12_HOURS("12 hours", "43200000"),
  BTN_1_DAY("1 day", "86400000"),
  BTN_2_DAYS("2 days", "172800000"),
  BTN_7_DAYS("7 days", "604800000"),
  BTN_4_WEEKS("4 weeks", "2419200000");

  private final String button;
  private final String value;

  TimeToRetain(String button, String value) {
    this.button = button;
    this.value = value;
  }
}
