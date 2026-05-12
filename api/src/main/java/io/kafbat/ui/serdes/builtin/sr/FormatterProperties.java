package io.kafbat.ui.serdes.builtin.sr;

import lombok.Builder;

@Builder
public record FormatterProperties(
    boolean showNullValues,
    boolean fullyQualifiedNames
) {
  public static final FormatterProperties EMPTY = builder().build();
}
