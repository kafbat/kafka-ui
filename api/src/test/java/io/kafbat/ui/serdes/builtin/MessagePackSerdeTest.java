package io.kafbat.ui.serdes.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.PropertyResolverImpl;
import io.kafbat.ui.serdes.RecordHeadersImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class MessagePackSerdeTest {
  private static final String EXPECTED_JSON_STRING = "{\"items\":[null,true,2]}";
  private static final byte[] EXPECTED_MESSAGE_PACK_BYTES = {
      (byte) 0x81,
      (byte) 0xa5,
      (byte) 0x69,
      (byte) 0x74,
      (byte) 0x65,
      (byte) 0x6d,
      (byte) 0x73,
      (byte) 0x93,
      (byte) 0xc0,
      (byte) 0xc3,
      (byte) 0x02
  };

  private Serde msgPackSerde;

  @BeforeEach
  void init() {
    msgPackSerde = new MessagePackSerde();
    msgPackSerde.configure(
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty()
    );
  }

  @ParameterizedTest
  @EnumSource
  void getSchemaReturnsEmpty(Serde.Target type) {
    assertThat(msgPackSerde.getSchema("anyTopic", type)).isEmpty();
  }

  @ParameterizedTest
  @EnumSource
  void canDeserializeReturnsTrueForAllInputs(Serde.Target type) {
    assertThat(msgPackSerde.canDeserialize("anyTopic", type)).isTrue();
  }

  @ParameterizedTest
  @EnumSource
  void deserializesMessagePackBytesAsJsonString(Serde.Target type) {
    var deserializer = msgPackSerde.deserializer("anyTopic", type);

    var result = deserializer.deserialize(new RecordHeadersImpl(), EXPECTED_MESSAGE_PACK_BYTES);

    assertThat(result.getResult()).isEqualTo(EXPECTED_JSON_STRING);
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.STRING);
    assertThat(result.getAdditionalProperties()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource
  void deserializesMessagePackBytesAsStringWithQuotes(Serde.Target type) {
    var deserializer = msgPackSerde.deserializer("anyTopic", type);

    var result = deserializer.deserialize(new RecordHeadersImpl(), new byte[] { (byte) 0xA4, 0x74, 0x65, 0x78, 0x74 });

    assertThat(result.getResult()).isEqualTo("\"text\"");
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.STRING);
    assertThat(result.getAdditionalProperties()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource
  void deserializesEmptyBytesThrowException(Serde.Target type) {
    var deserializer = msgPackSerde.deserializer("anyTopic", type);

    assertThatThrownBy(() -> deserializer.deserialize(new RecordHeadersImpl(), new byte[] {}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(MessagePackSerde.FAILED_TO_DESERIALIZE_MSGPACK_PAYLOAD);
  }

  @ParameterizedTest
  @EnumSource
  void deserializesInvalidMsgpackBytesThrowException(Serde.Target type) {
    var serializer = msgPackSerde.deserializer("anyTopic", type);

    assertThatThrownBy(() -> serializer.deserialize(new RecordHeadersImpl(), new byte[] { (byte) 0x93, 0x01, 0x02 }))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(MessagePackSerde.FAILED_TO_DESERIALIZE_MSGPACK_PAYLOAD);
  }

  @ParameterizedTest
  @EnumSource
  void canSerializeReturnsTrueForAllInput(Serde.Target type) {
    assertThat(msgPackSerde.canSerialize("anyTopic", type)).isTrue();
  }

  @ParameterizedTest
  @EnumSource
  void serializesJsonStringAsMessagePackBytes(Serde.Target type) {
    var serializer = msgPackSerde.serializer("anyTopic", type);

    byte[] bytes = serializer.serialize(" { \n\t\"items\" : [ null, true , 2]\n}  ");

    assertThat(bytes).isEqualTo(EXPECTED_MESSAGE_PACK_BYTES);
  }

  @ParameterizedTest
  @EnumSource
  void serializesBlankInputThrowsException(Serde.Target type) {
    var serializer = msgPackSerde.serializer("anyTopic", type);

    assertThatThrownBy(() -> serializer.serialize(" \n\t"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(MessagePackSerde.FAILED_TO_SERIALIZE_JSON_PAYLOAD);
  }

  @ParameterizedTest
  @EnumSource
  void serializesJsonWithMissingPartThrowsException(Serde.Target type) {
    var serializer = msgPackSerde.serializer("anyTopic", type);

    assertThatThrownBy(() -> serializer.serialize("{\"key\": "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(MessagePackSerde.FAILED_TO_SERIALIZE_JSON_PAYLOAD);
  }
}
