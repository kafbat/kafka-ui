package io.kafbat.ui.service.index;

import io.kafbat.ui.model.InternalTopic;
import java.util.List;

public interface TopicsIndex extends AutoCloseable {
  String FIELD_NAME = "name";
  String FIELD_INTERNAL = "internal";
  String FIELD_PARTITIONS = "partitions";
  String FIELD_REPLICATION = "replication";
  String FIELD_SIZE = "size";
  String FIELD_CONFIG_PREFIX = "config";

  default List<InternalTopic> find(String search, Boolean showInternal, Integer count) {
    return this.find(search, showInternal, FIELD_NAME, count);
  }

  List<InternalTopic> find(String search, Boolean showInternal, String sort, Integer count);
}
