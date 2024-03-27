package io.kafbat.ui.utilities;

import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtil {

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
