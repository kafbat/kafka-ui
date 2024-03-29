package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.RecordHeader;
import org.apache.kafka.common.header.Header;

public class RecordHeaderImpl implements RecordHeader {

  private final Header header;

  public RecordHeaderImpl(Header header) {
    this.header = header;
  }

  @Override
  public String key() {
    return header.key();
  }

  @Override
  public byte[] value() {
    return header.value();
  }
}
