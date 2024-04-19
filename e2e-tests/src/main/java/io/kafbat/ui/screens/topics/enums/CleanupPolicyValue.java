package io.kafbat.ui.screens.topics.enums;

import lombok.Getter;

@Getter
public enum CleanupPolicyValue {

  DELETE("delete", "Delete"),
  COMPACT("compact", "Compact"),
  COMPACT_DELETE("compact,delete", "Compact,Delete");

  private final String optionValue;
  private final String visibleText;

  CleanupPolicyValue(String optionValue, String visibleText) {
    this.optionValue = optionValue;
    this.visibleText = visibleText;
  }
}
