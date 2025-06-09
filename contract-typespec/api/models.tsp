model Metric {
  name?: string;
  labels?: Record<string>;
  value?: decimal;
}

enum ConfigSource {
  DYNAMIC_TOPIC_CONFIG,
  DYNAMIC_BROKER_LOGGER_CONFIG,
  DYNAMIC_BROKER_CONFIG,
  DYNAMIC_DEFAULT_BROKER_CONFIG,
  DYNAMIC_CLIENT_METRICS_CONFIG,
  STATIC_BROKER_CONFIG,
  DEFAULT_CONFIG,
  UNKNOWN,
}

enum SortOrder {
  ASC,
  DESC,
}





// model BrokerLogdirs {
//   name: string;
//   error: string;
//   topics: TopicLogdirs[];
// }

// model TopicLogdirs {
//   name?: string;
//   partitions?: TopicPartitionLogdir[];
// }

