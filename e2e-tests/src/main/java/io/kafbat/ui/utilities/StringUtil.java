package io.kafbat.ui.utilities;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.stream.IntStream;

public class StringUtil {

  public static String getOptionalString(String defaultValue, String... customValue) {
    return !isEmpty(customValue) && !isEmpty(customValue[0]) ? customValue[0] : defaultValue;
  }

  public static String clearString(String original) {
    if (original != null) {
      String cleanStr = original
          .replaceAll("\n", " ")
          .replaceAll("\r", " ")
          .replaceAll("\t", " ")
          .trim();
      while (cleanStr.contains("  ")) {
        cleanStr = cleanStr.replace("  ", " ");
      }
      return cleanStr;
    } else {
      return null;
    }
  }

  public static String getMixedCase(String original) {
    return IntStream.range(0, original.length())
        .mapToObj(i -> i % 2 == 0 ? Character.toUpperCase(original.charAt(i)) : original.charAt(i))
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  public static String getDuplicates(String toDuplicate, int count) {
    StringBuilder result = new StringBuilder();
    while (count > 0) {
      result.append(toDuplicate);
      count--;
    }
    return result.toString();
  }
}
