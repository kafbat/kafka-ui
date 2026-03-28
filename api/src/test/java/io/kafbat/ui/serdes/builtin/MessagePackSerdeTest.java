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
  private static final String TEST_STRING = "{\"items\":[null,true,2]}";
  private static final String TEST_STRING_WITH_INDENTS = " { \n\t\"items\" : [ null, true , 2]\n}  ";
  private static final String TEST_STRING_NOT_VALID_JSON = "{\"items\":[null,";
  private static final byte[] TEST_DATA_WITH_MISSING_BYTE = { (byte) 0x93, 0x01, 0x02 };
  private static final byte[] TEST_DATA = {
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
  void deserializesDataAsMessagePackBytes(Serde.Target type) {
    var deserializer = msgPackSerde.deserializer("anyTopic", type);
    var result = deserializer.deserialize(new RecordHeadersImpl(), TEST_DATA);
    assertThat(result.getResult()).isEqualTo(TEST_STRING);
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.STRING);
    assertThat(result.getAdditionalProperties()).isEmpty();
  }

  @ParameterizedTest
  @EnumSource
  void deserializesNotValidDataThrowsException(Serde.Target type) {
    var serializer = msgPackSerde.deserializer("anyTopic", type);
    assertThatThrownBy(() -> serializer.deserialize(new RecordHeadersImpl(), TEST_DATA_WITH_MISSING_BYTE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to deserialize MessagePack payload");
  }

  @ParameterizedTest
  @EnumSource
  void canSerializeReturnsTrueForAllInput(Serde.Target type) {
    assertThat(msgPackSerde.canSerialize("anyTopic", type)).isTrue();
  }

  @ParameterizedTest
  @EnumSource
  void serializesDataAsMessagePackBytes(Serde.Target type) {
    var serializer = msgPackSerde.serializer("anyTopic", type);
    byte[] bytes = serializer.serialize(TEST_STRING_WITH_INDENTS);
    assertThat(bytes).isEqualTo(TEST_DATA);
  }

  @ParameterizedTest
  @EnumSource
  void serializesNotValidDataThrowsException(Serde.Target type) {
    var serializer = msgPackSerde.serializer("anyTopic", type);
    assertThatThrownBy(() -> serializer.serialize(TEST_STRING_NOT_VALID_JSON))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to parse JSON payload");
  }
}
