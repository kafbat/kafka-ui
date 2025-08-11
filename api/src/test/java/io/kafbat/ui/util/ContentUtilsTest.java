package io.kafbat.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public class ContentUtilsTest {

  private static byte[] toBytes(Short value) {
    ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
    buffer.putShort(value);
    return buffer.array();
  }

  private static byte[] toBytes(Integer value) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(value);
    return buffer.array();
  }

  private static byte[] toBytes(Long value) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(value);
    return buffer.array();
  }

  @Test
  void testHeaderValueStringUtf8() {
    String testValue = "Test";

    assertEquals(testValue, ContentUtils.convertToString(testValue.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testHeaderValueInteger() {
    int testValue = 1;
    assertEquals(String.valueOf(testValue), ContentUtils.convertToString(toBytes(testValue)));
  }

  @Test
  void testHeaderValueLong() {
    long testValue = 111L;

    assertEquals(String.valueOf(testValue), ContentUtils.convertToString(toBytes(testValue)));
  }

  @Test
  void testHeaderValueShort() {
    short testValue = 10;

    assertEquals(String.valueOf(testValue), ContentUtils.convertToString(toBytes(testValue)));
  }

  @Test
  void testHeaderValueLongStringUtf8() {
    String testValue = RandomStringUtils.secure().next(10000, true, false);

    assertEquals(testValue, ContentUtils.convertToString(testValue.getBytes(StandardCharsets.UTF_8)));
  }

}
