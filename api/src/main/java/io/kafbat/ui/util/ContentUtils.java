package io.kafbat.ui.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Inspired by: https://github.com/tchiotludo/akhq/blob/dev/src/main/java/org/akhq/utils/ContentUtils.java
 */
public class ContentUtils {
  private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

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
    int i = 0;
    while (i < value.length) {
      int b = value[i] & 0xFF;
      int numBytes;
      if ((b & 0x80) == 0) {
        // 1-byte (ASCII)
        numBytes = 1;
      } else if ((b & 0xE0) == 0xC0) {
        // 2-byte sequence
        numBytes = 2;
      } else if ((b & 0xF0) == 0xE0) {
        // 3-byte sequence
        numBytes = 3;
      } else if ((b & 0xF8) == 0xF0) {
        // 4-byte sequence
        numBytes = 4;
      } else {
        // Invalid first byte
        return false;
      }
      if (i + numBytes > value.length) {
        return false;
      }
      // Check continuation bytes
      for (int j = 1; j < numBytes; j++) {
        if ((value[i + j] & 0xC0) != 0x80) {
          return false;
        }
      }
      i += numBytes;
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
