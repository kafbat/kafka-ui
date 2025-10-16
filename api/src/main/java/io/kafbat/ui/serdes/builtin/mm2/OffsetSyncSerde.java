package io.kafbat.ui.serdes.builtin.mm2;

import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Type;

@Slf4j
public class OffsetSyncSerde extends MirrorMakerSerde implements BuiltInSerde {
  public static final String NAME = "mm2-OffsetSync";
  public static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("mm2-offset-syncs\\..*\\.internal");

  private static final Schema VALUE_SCHEMA;
  private static final Schema KEY_SCHEMA;

  static {
    VALUE_SCHEMA = new Schema(
        new Field("upstreamOffset", Type.INT64),
        new Field("offset", Type.INT64)
    );
    KEY_SCHEMA = new Schema(
        new Field("topic", Type.STRING),
        new Field("partition", Type.INT32)
    );
  }

  public OffsetSyncSerde() {
    super(false);
  }

  @Override
  protected Schema getKeySchema() {
    return KEY_SCHEMA;
  }

  @Override
  protected Optional<Schema> getValueSchema() {
    return Optional.of(VALUE_SCHEMA);
  }
}
