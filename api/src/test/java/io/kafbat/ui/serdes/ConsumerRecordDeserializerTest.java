package io.kafbat.ui.serdes;

import static io.kafbat.ui.serde.api.DeserializeResult.Type.STRING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kafbat.ui.model.TopicMessageDTO;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.Test;

class ConsumerRecordDeserializerTest {

  @Test
  void dataMaskingAppliedOnDeserializedMessage() {
    UnaryOperator<TopicMessageDTO> maskerMock = mock();
    Serde.Deserializer deser = (headers, data) -> new DeserializeResult("test", STRING, Map.of());

    var recordDeser = new ConsumerRecordDeserializer("test", deser, "test", deser, "test", deser, deser, maskerMock);
    recordDeser.deserialize(record());

    verify(maskerMock).apply(any(TopicMessageDTO.class));
  }

  @Test
  void deserializeWithMultipleHeaderValues() {
    UnaryOperator<TopicMessageDTO> maskerMock = mock();
    when(maskerMock.apply(any(TopicMessageDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Serde.Deserializer deser = (headers, data) -> new DeserializeResult("test", STRING, Map.of());

    var recordDeser = new ConsumerRecordDeserializer("test", deser, "test", deser, "test", deser, deser, maskerMock);
    ConsumerRecord<Bytes, Bytes> record = record();
    record.headers().add("headerKey", "headerValue1".getBytes());
    record.headers().add("headerKey", "headerValue2".getBytes());
    TopicMessageDTO message = recordDeser.deserialize(record);

    Map<String, List<String>> headers = message.getHeaders();
    assertEquals(1, headers.size());
    assertEquals(List.of("headerValue1", "headerValue2"), headers.get("headerKey"));
  }

  @Test
  void deserializeWithMixedSingleAndMultipleHeaderValues() {
    UnaryOperator<TopicMessageDTO> maskerMock = mock();
    when(maskerMock.apply(any(TopicMessageDTO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Serde.Deserializer deser = (headers, data) -> new DeserializeResult("test", STRING, Map.of());

    var recordDeser = new ConsumerRecordDeserializer("test", deser, "test", deser, "test", deser, deser, maskerMock);
    ConsumerRecord<Bytes, Bytes> record = record();
    record.headers().add("headerKey1", "singleValue".getBytes());
    record.headers().add("headerKey2", "multiValue1".getBytes());
    record.headers().add("headerKey2", "multiValue2".getBytes());
    TopicMessageDTO message = recordDeser.deserialize(record);

    Map<String, List<String>> headers = message.getHeaders();
    assertEquals(1, headers.get("headerKey1").size());
    assertEquals(List.of("singleValue"), headers.get("headerKey1"));
    assertEquals(2, headers.get("headerKey2").size());
    assertEquals(List.of("multiValue1", "multiValue2"), headers.get("headerKey2"));
  }

  private ConsumerRecord<Bytes, Bytes> record() {
    return new ConsumerRecord<>("t", 1, 1L, Bytes.wrap("t".getBytes()), Bytes.wrap("t".getBytes()));
  }

}
