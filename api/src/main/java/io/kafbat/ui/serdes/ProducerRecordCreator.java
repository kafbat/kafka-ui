package io.kafbat.ui.serdes;

import io.kafbat.ui.serde.api.Serde;
import java.util.Map;
import jakarta.annotation.Nullable;
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

    return new ProducerRecord<>(
        topic,
        partition,
        key == null ? null : keySerializer.serialize(key, kafkaHeaders),
        value == null ? null : valuesSerializer.serialize(value, kafkaHeaders),
        kafkaHeaders
    );
  }

  private Headers createHeaders(Map<String, String> clientHeaders) {
    RecordHeaders headers = new RecordHeaders();
    if (clientHeaders != null) {
      clientHeaders.forEach((k, v) -> headers.add(new RecordHeader(k, v == null ? null : v.getBytes())));
    }
    return headers;
  }

}
