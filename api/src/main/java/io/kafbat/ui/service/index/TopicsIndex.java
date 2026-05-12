package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.util.List;
import java.util.Map;

public interface TopicsIndex extends AutoCloseable {
  String FIELD_NAME = "name";
  String FIELD_INTERNAL = "internal";
  String FIELD_PARTITIONS = "partitions";
  String FIELD_REPLICATION = "replication";
  String FIELD_SIZE = "size";
  String FIELD_CONFIG_PREFIX = "config";

  enum FieldType {
    STRING,
    INT,
    LONG,
    BOOLEAN
  }

  Map<String, FieldType> FIELD_TYPES = Map.of(
      FIELD_NAME, FieldType.STRING,
      FIELD_INTERNAL, FieldType.BOOLEAN,
      FIELD_PARTITIONS, FieldType.INT,
      FIELD_REPLICATION, FieldType.INT,
      FIELD_SIZE, FieldType.LONG,
      FIELD_CONFIG_PREFIX, FieldType.STRING
  );

  default List<InternalTopic> find(String search, Boolean showInternal, boolean fts, Integer count) {
    return this.find(search, showInternal, FIELD_NAME, fts, count);
  }

  List<InternalTopic> find(String search, Boolean showInternal, String sort, boolean fts, Integer count);
}
