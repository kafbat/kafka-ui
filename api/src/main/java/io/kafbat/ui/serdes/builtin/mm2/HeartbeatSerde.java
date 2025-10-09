package io.kafbat.ui.serdes.builtin.mm2;

import io.kafbat.ui.serdes.BuiltInSerde;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Type;

@Slf4j
public class HeartbeatSerde extends MirrorMakerSerde implements BuiltInSerde {
  public static final String NAME = "mm2-Heartbeat";
  public static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("heartbeats");

  private static final String SOURCE_CLUSTER_ALIAS_KEY = "sourceClusterAlias";
  private static final String TARGET_CLUSTER_ALIAS_KEY = "targetClusterAlias";
  private static final String TIMESTAMP_KEY = "timestamp";

  private static final Schema VALUE_SCHEMA_V0 = new Schema(
      new Field(TIMESTAMP_KEY, Type.INT64));

  private static final Schema KEY_SCHEMA = new Schema(
      new Field(SOURCE_CLUSTER_ALIAS_KEY, Type.STRING),
      new Field(TARGET_CLUSTER_ALIAS_KEY, Type.STRING));

  public HeartbeatSerde() {
    super(true);
  }

  protected Schema getKeySchema() {
    return KEY_SCHEMA;
  }

  @Override
  protected Optional<Schema> getVersionedValueSchema(short version) {
    if (version == 0) {
      return Optional.of(VALUE_SCHEMA_V0);
    } else {
      log.warn("Unsupported version of HeartbeatSerde: {}", version);
      return Optional.empty();
    }
  }
}
