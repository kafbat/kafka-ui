package io.kafbat.ui.serdes.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.PropertyResolverImpl;
import io.kafbat.ui.serdes.RecordHeadersImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class MessagePackSerdeTest {
  private static final String TEST_STRING = "{\"items\":[null,true,2]}";
  private static final byte[] TEST_BYTES = {
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
  void canSerializeReturnsFalseForAllInput(Serde.Target type) {
    assertThat(msgPackSerde.canSerialize("anyTopic", type)).isFalse();
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
    var result = deserializer.deserialize(new RecordHeadersImpl(), TEST_BYTES);
    assertThat(result.getResult()).isEqualTo(TEST_STRING);
    assertThat(result.getType()).isEqualTo(DeserializeResult.Type.STRING);
    assertThat(result.getAdditionalProperties()).isEmpty();
  }
}
