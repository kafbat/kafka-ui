package io.kafbat.ui.serdes.builtin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.kafbat.ui.serde.api.DeserializeResult;
import io.kafbat.ui.serdes.BuiltInSerde;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.kafka.common.protocol.types.ArrayOf;
import org.apache.kafka.common.protocol.types.BoundField;
import org.apache.kafka.common.protocol.types.CompactArrayOf;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;

public class TransactionStateSerde extends StructSerde implements BuiltInSerde {

  private static final JsonMapper TX_JSON_MAPPER = createMapper();

  private static JsonMapper createMapper() {
    var module = new SimpleModule();
    module.addSerializer(Struct.class, new JsonSerializer<Struct>() {
          @Override
          public void serialize(Struct value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            for (BoundField field : value.schema().fields()) {
              var fieldValue = value.get(field);
              var fieldName = field.def.name;
              if (TRANSACTION_STATUS.equals(fieldName) && fieldValue instanceof Number n) {
                gen.writeObjectField(fieldName, TransactionStatus.nameForId(n.intValue()));
              } else {
                gen.writeObjectField(fieldName, fieldValue);
              }
            }
            gen.writeEndObject();
          }
        }
    );
    var mapper = new JsonMapper();
    mapper.registerModule(module);
    return mapper;
  }

  public static final String TOPIC_NAME = "__transaction_state";
  public static final String NAME = "__transaction_state";

  private static final String TRANSACTIONAL_ID = "transaction_id";
  private static final String PRODUCER_ID = "producer_id";
  private static final String PRODUCER_EPOCH = "producer_epoch";
  private static final String TRANSACTION_TIMEOUT_MS = "transaction_timeout_ms";
  private static final String TRANSACTION_STATUS = "transaction_status";
  private static final String TRANSACTION_PARTITIONS = "transaction_partitions";
  private static final String TOPIC = "topic";
  private static final String PARTITION_IDS = "partition_ids";
  private static final String TRANSACTION_LAST_UPDATE_TIMESTAMP_MS = "transaction_last_update_timestamp_ms";
  private static final String TRANSACTION_START_TIMESTAMP_MS = "transaction_start_timestamp_ms";

  @Override
  public boolean canDeserialize(String topic, Target type) {
    return topic.equals(TOPIC_NAME);
  }

  @Override
  public Deserializer deserializer(String topic, Target type) {
    return switch (type) {
      case KEY -> keyDeserializer();
      case VALUE -> valueDeserializer();
    };
  }

  private Deserializer keyDeserializer() {
    final Schema transactionKeySchema = new Schema(
        new Field(TRANSACTIONAL_ID, Type.STRING, "")
    );

    return (headers, data) -> {
      var bb = ByteBuffer.wrap(data);
      short version = bb.getShort();
      return new DeserializeResult(
          toJson(transactionKeySchema.read(bb)),
          DeserializeResult.Type.JSON,
          Map.of()
      );
    };
  }

  private Deserializer valueDeserializer() {
    final Schema transactionLogSchemaV0 =
        new Schema(
            new Field(PRODUCER_ID, Type.INT64, ""),
            new Field(PRODUCER_EPOCH, Type.INT16, ""),
            new Field(TRANSACTION_TIMEOUT_MS, Type.INT32, ""),
            new Field(TRANSACTION_STATUS, Type.INT8, ""),
            new Field(TRANSACTION_PARTITIONS, ArrayOf.nullable(new Schema(
                new Field(TOPIC, Type.STRING, ""),
                new Field(PARTITION_IDS, new ArrayOf(Type.INT32), "")
            )), ""),
            new Field(TRANSACTION_LAST_UPDATE_TIMESTAMP_MS, Type.INT64, ""),
            new Field(TRANSACTION_START_TIMESTAMP_MS, Type.INT64, "")
        );

    final Schema transactionLogSchemaV1 =
        new Schema(
            new Field(PRODUCER_ID, Type.INT64, ""),
            new Field(PRODUCER_EPOCH, Type.INT16, ""),
            new Field(TRANSACTION_TIMEOUT_MS, Type.INT32, ""),
            new Field(TRANSACTION_STATUS, Type.INT8, ""),
            new Field(TRANSACTION_PARTITIONS, new CompactArrayOf(new Schema(
                new Field(TOPIC, Type.COMPACT_STRING, ""),
                new Field(PARTITION_IDS, CompactArrayOf.nullable(Type.INT32), ""),
                Field.TaggedFieldsSection.of()
            )), ""),
            new Field(TRANSACTION_LAST_UPDATE_TIMESTAMP_MS, Type.INT64, ""),
            new Field(TRANSACTION_START_TIMESTAMP_MS, Type.INT64, ""),
            Field.TaggedFieldsSection.of()
        );

    return (headers, data) -> {
      String result;
      var bb = ByteBuffer.wrap(data);
      short version = bb.getShort();
      System.out.println("TransactionLog: version: " + version);
      System.out.println("TransactionLog: " + bb.remaining());
      result = toJson(
          switch (version) {
            case 0 -> transactionLogSchemaV0.read(bb);
            case 1 -> transactionLogSchemaV1.read(bb);
            default -> throw new IllegalArgumentException("Unrecognized version: " + version);
          }
      );
      System.out.println("TransactionLog: " + bb.remaining());


      if (bb.remaining() != 0) {
        throw new IllegalArgumentException(
            "Message buffer is not read to the end, which is likely means message is unrecognized");
      }
      return new DeserializeResult(
          result,
          DeserializeResult.Type.JSON,
          Map.of()
      );
    };
  }

  @Override
  @SneakyThrows
  protected String toJson(Struct s) {
    return TX_JSON_MAPPER.writeValueAsString(s);
  }

  public enum TransactionStatus {
    EMPTY(0),
    ONGOING(1),
    PREPARE_COMMIT(2),
    PREPARE_ABORT(3),
    COMPLETE_COMMIT(4),
    COMPLETE_ABORT(5),
    DEAD(6),
    PREPARE_EPOCH_FENCE(7);

    private final int id;

    TransactionStatus(int id) {
      this.id = id;
    }

    static String nameForId(int id) {
      for (var status : values()) {
        if (status.id == id) {
          return status.name();
        }
      }
      return "UNKNOWN(" + id + ")";
    }
  }
}
