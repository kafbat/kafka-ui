package io.kafbat.ui.serdes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import io.kafbat.ui.serde.api.Serde;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;

class ProducerRecordCreatorTest {

  @Test
  void createWithHeaders() {
    Serde.Serializer keySerializer = mock(Serde.Serializer.class);
    Serde.Serializer valueSerializer = mock(Serde.Serializer.class);

    ProducerRecordCreator recordCreator = new ProducerRecordCreator(keySerializer, valueSerializer);
    Map<String, List<String>> headers = Map.of(
        "headerKey1", List.of("headerValue1"),
        "headerKey2", List.of("headerValue2", "headerValue3")
    );
    ProducerRecord<byte[], byte[]> record = recordCreator.create("topic", 1, "key", "value", headers);

    assertNotNull(record.headers());
    assertEquals(3, record.headers().toArray().length);
    assertThat(record.headers()).containsExactlyInAnyOrder(
        new RecordHeader("headerKey1", "headerValue1".getBytes()),
        new RecordHeader("headerKey2", "headerValue2".getBytes()),
        new RecordHeader("headerKey2", "headerValue3".getBytes())
    );
  }
}
