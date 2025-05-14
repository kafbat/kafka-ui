package io.kafbat.ui.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Inspired by: https://github.com/tchiotludo/akhq/blob/dev/src/main/java/org/akhq/utils/ContentUtils.java
 */
public class ContentUtils {
  private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

  private static final Pattern UTF8_PATTERN = Pattern.compile("\\A(\n"
      + "  [\\x09\\x0A\\x0D\\x20-\\x7E]             # ASCII\\n"
      + "| [\\xC2-\\xDF][\\x80-\\xBF]               # non-overlong 2-byte\n"
      + "|  \\xE0[\\xA0-\\xBF][\\x80-\\xBF]         # excluding overlongs\n"
      + "| [\\xE1-\\xEC\\xEE\\xEF][\\x80-\\xBF]{2}  # straight 3-byte\n"
      + "|  \\xED[\\x80-\\x9F][\\x80-\\xBF]         # excluding surrogates\n"
      + "|  \\xF0[\\x90-\\xBF][\\x80-\\xBF]{2}      # planes 1-3\n"
      + "| [\\xF1-\\xF3][\\x80-\\xBF]{3}            # planes 4-15\n"
      + "|  \\xF4[\\x80-\\x8F][\\x80-\\xBF]{2}      # plane 16\n"
      + ")*\\z", Pattern.COMMENTS);

  private ContentUtils() {
  }

  /**
   * Detects if bytes contain a UTF-8 string or something else
   * Source: https://stackoverflow.com/questions/1193200/how-can-i-check-whether-a-byte-array-contains-a-unicode-string-in-java
   * @param value the bytes to test for a UTF-8 encoded {@code java.lang.String} value
   * @return  true, if the byte[] contains a UTF-8 encode  {@code java.lang.String}
   */
  public static boolean isValidUtf8(byte[] value) {
    //If the array is too long, it throws a StackOverflowError due to the regex, so we assume it is a String.
    if (value.length <= 1000) {
      String phonyString = new String(value, StandardCharsets.ISO_8859_1);
      return UTF8_PATTERN.matcher(phonyString).matches();
    }
    return true;
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
          try {
            valueAsString = String.valueOf(ContentUtils.asLong(value));
          } catch (Exception e) {
            try {
              valueAsString = String.valueOf(ContentUtils.asInt(value));
            } catch (Exception ex) {
              valueAsString = String.valueOf(ContentUtils.asShort(value));
            }
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
