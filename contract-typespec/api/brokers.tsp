import "@typespec/openapi";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/brokers")
@tag("Brokers")
interface BrokersApi {
  @get
  @operationId("getBrokers")
  @summary("getBrokers")
  getBrokers(@path clusterName: string): Broker[];

  @get
  @operationId("getBrokersCsv")
  @summary("getBrokersCsv")
  @route("/csv")
  getBrokersCsv(@path clusterName: string): CsvResponse;

  @get
  @route("/{id}/configs")
  @operationId("getBrokerConfig")
  @summary("getBrokerConfig")
  getBrokerConfig(@path clusterName: string, @path id: int32): BrokerConfig[];

  @put
  @route("/{id}/configs/{name}")
  @operationId("updateBrokerConfigByName")
  @summary("updateBrokerConfigByName")
  updateBrokerConfigByName(
    @path clusterName: string,
    @path id: int32,
    @path name: string,
    @body config: BrokerConfigItem,
  ): void | ApiBadRequestResponse;

  @get
  @route("/{id}/metrics")
  @operationId("getBrokersMetrics")
  @summary("getBrokersMetrics")
  getBrokersMetrics(@path clusterName: string, @path id: int32): BrokerMetrics;

  @get
  @route("/logdirs")
  @operationId("getAllBrokersLogdirs")
  @summary("getAllBrokersLogdirs")
  getAllBrokersLogdirs(
    @path clusterName: string,
    @query broker?: int32[],
  ): BrokersLogdirs[];

  @patch(#{implicitOptionality: true})
  @route("/{id}/logdirs")
  @operationId("updateBrokerTopicPartitionLogDir")
  @summary("updateBrokerTopicPartitionLogDir")
  updateBrokerTopicPartitionLogDir(
    @path clusterName: string,
    @path id: int32,
    @body update: BrokerLogdirUpdate,
  ): void | ApiBadRequestResponse;
}

model Broker {
  id: int32;
  host?: string;
  port?: int32;
  bytesInPerSec?: float64;
  bytesOutPerSec?: float64;
  partitionsLeader?: int32;
  partitions?: int32;
  inSyncPartitions?: int32;
  partitionsSkew?: float64;
  leadersSkew?: float64;
}

model BrokerLogdirUpdate {
  topic?: string;
  partition?: int32;
  logDir?: string;
}

model BrokerConfig {
  name: string;
  value: string;
  source: ConfigSource;
  isSensitive: boolean;
  isReadOnly: boolean;
  synonyms?: ConfigSynonym[];
}

model BrokerConfigItem {
  value?: string;
}

model BrokerLogdirs {
    name?: string;
    error?: string;
    topics?: TopicLogdirs[];
}

model TopicLogdirs {
    name?: string;
    partitions?: TopicPartitionLogdir[];
}

model BrokerMetrics {
  segmentSize?: int64;
  segmentCount?: int32;
  metrics?: Metric[];
}

model BrokersLogdirs {
  name?: string;
  error?: string;
  topics?: BrokerTopicLogdirs[];
}

model BrokerTopicLogdirs {
  name?: string;
  partitions?: BrokerTopicPartitionLogdir[];
}

model BrokerTopicPartitionLogdir {
  ...TopicPartitionLogdir;
  broker?: int32;
}

model TopicPartitionLogdir {
  partition?: int32;
  size?: int64;
  offsetLag?: int64;
}
