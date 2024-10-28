package io.kafbat.ui.serdes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.kafbat.ui.exception.ValidationException;
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
    Map<String, Object> headersMap = Map.of(
        "headerKey1", "headerValue1",
        "headerKey2", List.of("headerValue2", "headerValue3")
    );
    ProducerRecord<byte[], byte[]> record = recordCreator.create("topic", 1, "key", "value", headersMap);

    assertNotNull(record.headers());
    assertEquals(3, record.headers().toArray().length);
    assertThat(record.headers()).containsExactlyInAnyOrder(
        new RecordHeader("headerKey1", "headerValue1".getBytes()),
        new RecordHeader("headerKey2", "headerValue2".getBytes()),
        new RecordHeader("headerKey2", "headerValue3".getBytes())
    );
  }

  @Test
  void createWithInvalidHeaderValue() {
    Serde.Serializer keySerializer = mock(Serde.Serializer.class);
    Serde.Serializer valueSerializer = mock(Serde.Serializer.class);

    ProducerRecordCreator recordCreator = new ProducerRecordCreator(keySerializer, valueSerializer);
    Map<String, Object> invalidHeaders = Map.of("headerKey", Map.of("invalid", "value"));

    assertThrows(ValidationException.class, () ->
        recordCreator.create("topic", 1, "key", "value", invalidHeaders));
  }
}
