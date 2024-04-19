package io.kafbat.ui.utilities;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.Objects;

public class IntUtil {

  public static int getIntegerFromString(String original, boolean validate) {
    String cleanStr = isEmpty(original) ? null
        : original.replaceAll("(\\D+)", "");
    int result = 0;
    try {
      result = Integer.parseInt(Objects.requireNonNull(cleanStr));
    } catch (Throwable throwable) {
      if (validate) {
        throw new IllegalArgumentException(String.format("Unable to parse string '%s'", original));
      }
    }
    return result;
  }
}
