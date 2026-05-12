package io.kafbat.ui.serde.api;

/**
 * Header of kafka record.
 */
public interface RecordHeader {

  /**
   * Header key.
   * @return header key.
   */
  String key();

  /**
   * Header value.
   * @return header value.
   */
  byte[] value();

}
