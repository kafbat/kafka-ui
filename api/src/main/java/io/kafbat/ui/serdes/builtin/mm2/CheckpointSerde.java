package io.kafbat.ui.serdes.builtin.mm2;

import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Type;

@Slf4j
public class CheckpointSerde extends MirrorMakerSerde implements BuiltInSerde {
  public static final String NAME = "mm2-Checkpoint";
  public static final Pattern TOPIC_NAME_PATTERN = Pattern.compile(".*\\.checkpoints\\.internal");

  private static final String TOPIC_KEY = "topic";
  private static final String PARTITION_KEY = "partition";
  private static final String CONSUMER_GROUP_ID_KEY = "group";
  private static final String UPSTREAM_OFFSET_KEY = "upstreamOffset";
  private static final String DOWNSTREAM_OFFSET_KEY = "offset";
  private static final String METADATA_KEY = "metadata";

  private static final Schema VALUE_SCHEMA_V0 = new Schema(
      new Field(UPSTREAM_OFFSET_KEY, Type.INT64),
      new Field(DOWNSTREAM_OFFSET_KEY, Type.INT64),
      new Field(METADATA_KEY, Type.STRING));

  private static final Schema KEY_SCHEMA = new Schema(
      new Field(CONSUMER_GROUP_ID_KEY, Type.STRING),
      new Field(TOPIC_KEY, Type.STRING),
      new Field(PARTITION_KEY, Type.INT32));

  public CheckpointSerde() {
    super(true);
  }

  @Override
  protected Schema getKeySchema() {
    return KEY_SCHEMA;
  }

  @Override
  protected Optional<Schema> getVersionedValueSchema(short version) {
    if (version == 0) {
      return Optional.of(VALUE_SCHEMA_V0);
    } else {
      log.warn("Unsupported version of CheckpointSerde: {}", version);
      return Optional.empty();
    }
  }

}
