package io.kafbat.ui.serdes;

import com.google.common.collect.Iterators;
import io.kafbat.ui.serde.api.RecordHeader;
import io.kafbat.ui.serde.api.RecordHeaders;
import java.util.Iterator;
import org.apache.kafka.common.header.Headers;


public class RecordHeadersImpl implements RecordHeaders {

  private final Headers headers;

  public RecordHeadersImpl() {
    this(new org.apache.kafka.common.header.internals.RecordHeaders());
  }

  public RecordHeadersImpl(Headers headers) {
    this.headers = headers;
  }

  @Override
  public Iterator<RecordHeader> iterator() {
    return Iterators.transform(headers.iterator(), RecordHeaderImpl::new);
  }
}
