package io.kafbat.ui.screens.ksqldb.enums;

import lombok.Getter;

@Getter
public enum KsqlMenuTabs {

  TABLES("Table"),
  STREAMS("Streams");

  private final String value;

  KsqlMenuTabs(String value) {
    this.value = value;
  }
}
