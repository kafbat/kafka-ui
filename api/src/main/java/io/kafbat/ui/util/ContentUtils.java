package io.kafbat.ui.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Provides utility methods converting byte data to string representations.
 * Inspired by: https://github.com/tchiotludo/akhq/blob/dev/src/main/java/org/akhq/utils/ContentUtils.java
 */
public class ContentUtils {
  private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

  private static final CharsetDecoder UTF8_DECODER = StandardCharsets.UTF_8.newDecoder();

  private ContentUtils() {
  }

  /**
   * Detects if bytes contain a UTF-8 string or something else.
   * @param value the bytes to test for a UTF-8 encoded {@code java.lang.String} value
   * @return  true, if the byte[] contains a UTF-8 encode  {@code java.lang.String}
   */
  public static boolean isValidUtf8(byte[] value) {
    // Any data exceeding 10KB will be treated as a string.
    if (value.length > 10_000) {
      return true;
    }
    try {
      CharBuffer decode = UTF8_DECODER.decode(ByteBuffer.wrap(value));
      return decode.chars().allMatch(ContentUtils::isValidUtf8);
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isValidUtf8(int c) {
    // SKIP NULL Symbols
    if (c == 0) {
      return false;
    }
    // Well known symbols
    if (Character.isAlphabetic(c)
        || Character.isDigit(c)
        || Character.isWhitespace(c)
        || isEmoji(c)
    ) {
      return true;
    }
    // We could read only whitespace controls like
    return !Character.isISOControl(c);
  }

  public static boolean isEmoji(int codePoint) {
    return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) || // Emoticons
        (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) || // Miscellaneous Symbols and Pictographs
        (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) || // Transport and Map Symbols
        (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) || // Supplemental Symbols and Pictographs
        (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF) || // Symbols and Pictographs Extended-A
        (codePoint >= 0x2600 && codePoint <= 0x26FF) ||   // Misc Symbols
        (codePoint >= 0x2700 && codePoint <= 0x27FF);     // Dingbats
  }

  /**
   * Converts bytes to long.
   *
   * @param value the bytes to convert in to a long
   * @return the long build from the given bytes
   */
  public static Long asLong(byte[] value) {
    return value != null ? ByteBuffer.wrap(value).getLong() : null;
  }

  /**
   * Converts the given bytes to {@code int}.
   *
   * @param value the bytes to convert into a {@code int}
   * @return the {@code int} build from the given bytes
   */
  public static Integer asInt(byte[] value) {
    return value != null ? ByteBuffer.wrap(value).getInt() : null;
  }

  /**
   * Converts the given bytes to {@code short}.
   *
   * @param value the bytes to convert into a {@code short}
   * @return the {@code short} build from the given bytes
   */
  public static Short asShort(byte[] value) {
    return value != null ? ByteBuffer.wrap(value).getShort() : null;
  }

  /**
   * Converts the given bytes either into a {@code java.lang.string}, {@code int},
   * {@code long} or {@code short} depending on the content it contains.
   * @param value     the bytes to convert
   * @return  the value as an  {@code java.lang.string}, {@code int}, {@code long} or {@code short}
   */
  public static String convertToString(byte[] value) {
    String valueAsString = null;

    if (value != null) {
      try {
        if (ContentUtils.isValidUtf8(value)) {
          valueAsString = new String(value);
        } else {
          if (value.length == Long.BYTES) {
            valueAsString = String.valueOf(ContentUtils.asLong(value));
          } else if (value.length == Integer.BYTES) {
            valueAsString = String.valueOf(ContentUtils.asInt(value));
          } else if (value.length == Short.BYTES) {
            valueAsString = String.valueOf(ContentUtils.asShort(value));
          } else {
            valueAsString = bytesToHex(value);
          }
        }
      } catch (Exception ex) {
        // Show the header as hexadecimal string
        valueAsString = bytesToHex(value);
      }
    }
    return valueAsString;
  }

  // https://stackoverflow.com/questions/9655181/java-convert-a-byte-array-to-a-hex-string
  public static String bytesToHex(byte[] bytes) {
    byte[] hexChars = new byte[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }
    return new String(hexChars, StandardCharsets.UTF_8);
  }

}
