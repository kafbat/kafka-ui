package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.Serde;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

@RequiredArgsConstructor
public class ProducerRecordCreator {

  private final Serde.Serializer keySerializer;
  private final Serde.Serializer valuesSerializer;

  public ProducerRecord<byte[], byte[]> create(String topic,
                                               @Nullable Integer partition,
                                               @Nullable String key,
                                               @Nullable String value,
                                               @Nullable Map<String, String> headers) {

    Headers kafkaHeaders = createHeaders(headers);
    byte[] keyBytes = keySerializer.serialize(key, kafkaHeaders);
    byte[] valueBytes = valuesSerializer.serialize(value, kafkaHeaders);

    return new ProducerRecord<>(
        topic,
        partition,
        keyBytes,
        valueBytes,
        kafkaHeaders
    );
  }

  private Headers createHeaders(Map<String, String> clientHeaders) {
    RecordHeaders headers = new RecordHeaders();
    if (clientHeaders != null) {
      clientHeaders.forEach((k, v) -> headers.add(new RecordHeader(k, v.getBytes())));
    }
    return headers;
  }

}
