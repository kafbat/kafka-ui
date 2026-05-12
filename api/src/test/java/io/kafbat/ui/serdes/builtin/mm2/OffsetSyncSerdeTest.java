package io.kafbat.ui.serdes.builtin.mm2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serde.api.Serde;
import io.kafbat.ui.serdes.PropertyResolverImpl;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OffsetSyncSerdeTest extends MirrorMakerSerdesAbstractTest {

  private static final OffsetSyncSerde SERDE = new OffsetSyncSerde();

  @BeforeEach
  void init() {
    SERDE.configure(
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty(),
        PropertyResolverImpl.empty()
    );
  }

  @Test
  void testCanDeserialize() {
    assertTrue(SERDE.canDeserialize(TOPIC, Serde.Target.KEY));
    assertTrue(SERDE.canDeserialize(TOPIC, Serde.Target.VALUE));
  }

  @Test
  void testDeserializeKey() throws JsonProcessingException {
    var key = decodeBase64("AAl0b3BpY25hbWUAAAAA");
    var expected = Map.of(
        "partition", 0,
        "topic", "topicname"
    );

    var result = SERDE.deserializer(TOPIC, Serde.Target.KEY).deserialize(HEADERS, key);
    var resultMap = jsonToMap(result.getResult());

    assertEquals(DeserializeResult.Type.JSON, result.getType());
    assertEquals(expected, resultMap);
  }

  @Test
  void testDeserializeValue() throws JsonProcessingException {
    var value = decodeBase64("AAAAAAACXsoAAAAAAAHMfw==");
    var expected = Map.of(
        "offset", 117887,
        "upstreamOffset", 155338
    );

    var result = SERDE.deserializer(TOPIC, Serde.Target.VALUE).deserialize(HEADERS, value);
    var resultMap = jsonToMap(result.getResult());

    assertEquals(DeserializeResult.Type.JSON, result.getType());
    assertEquals(expected, resultMap);
  }

}
