import "@typespec/openapi";
import "./responses.tsp";
import "./models.tsp";
import "./acls.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/topics")
@tag("Topics")
interface TopicsApi {
  @get
  @operationId("getTopics")
  @summary("getTopics")
  @doc("List topics in a Kafka cluster with pagination, search, and sorting support.")
  getTopics(
    @path clusterName: string,
    @query page?: int32,
    @query perPage?: int32,
    @query showInternal?: boolean,
    @query search?: string,
    @query orderBy?: TopicColumnsToSort,
    @query sortOrder?: SortOrder,
    @query fts?: boolean
  ): TopicsResponse;

  @get
  @operationId("getTopicsCsv")
  @summary("getTopics")
  @route("csv")
  getTopicsCsv(
    @path clusterName: string,
    @query showInternal?: boolean,
    @query search?: string,
    @query orderBy?: TopicColumnsToSort,
    @query sortOrder?: SortOrder,
    @query fts?: boolean
  ): CsvResponse;

  @post
  @operationId("createTopic")
  @summary("createTopic")
  @doc("Create a new topic in a Kafka cluster with specified partitions, replication factor, and configs.")
  createTopic(
    @path clusterName: string,
    @body topic: TopicCreation,
  ): ApiCreatedResponse<Topic>;

  @post
  @route("/{topicName}/clone")
  @operationId("cloneTopic")
  @summary("cloneTopic")
  @doc("Clone an existing topic to a new topic with the same configuration.")
  cloneTopic(
    @path clusterName: string,
    @path topicName: string,
    @query newTopicName: string,
  ): ApiCreatedResponse<Topic> | ApiNotFoundResponse;

  @get
  @route("/{topicName}/analysis")
  @operationId("getTopicAnalysis")
  @summary("getTopicAnalysis")
  @doc("Get the analysis results for a specific topic.")
  getTopicAnalysis(
    @path clusterName: string,
    @path topicName: string,
  ): TopicAnalysis | ApiNotFoundResponse;

  @post
  @route("/{topicName}/analysis")
  @operationId("analyzeTopic")
  @summary("analyzeTopic")
  @doc("Start analysis of a topic to gather statistics about messages.")
  analyzeTopic(
    @path clusterName: string,
    @path topicName: string,
  ): void | ApiNotFoundResponse;

  @delete
  @route("/{topicName}/analysis")
  @operationId("cancelTopicAnalysis")
  @summary("cancelTopicAnalysis")
  @doc("Cancel a running topic analysis.")
  cancelTopicAnalysis(
    @path clusterName: string,
    @path topicName: string,
  ): void | ApiNotFoundResponse;

  @get
  @route("/{topicName}")
  @operationId("getTopicDetails")
  @summary("getTopicDetails")
  @doc("Get detailed information about a specific topic including partitions and configuration.")
  getTopicDetails(
    @path clusterName: string,
    @path topicName: string,
  ): TopicDetails;

  @post
  @route("/{topicName}")
  @operationId("recreateTopic")
  @summary("recreateTopic")
  @doc("Delete and recreate a topic with the same configuration.")
  recreateTopic(@path clusterName: string, @path topicName: string):
    | ApiCreatedResponse<Topic>
    | ApiTimeoutResponse
    | ApiNotFoundResponse
    | ApiBadRequestResponse;

  @patch(#{implicitOptionality: true})
  @route("/{topicName}")
  @operationId("updateTopic")
  @summary("updateTopic")
  @doc("Update the configuration of an existing topic.")
  updateTopic(
    @path clusterName: string,
    @path topicName: string,
    @body update: TopicUpdate,
  ): Topic;

  @delete
  @route("/{topicName}")
  @operationId("deleteTopic")
  @summary("deleteTopic")
  @doc("Delete a topic from a Kafka cluster.")
  deleteTopic(
    @path clusterName: string,
    @path topicName: string,
  ): void | NotFoundResponse;

  @get
  @route("/{topicName}/config")
  @operationId("getTopicConfigs")
  @summary("getTopicConfigs")
  @doc("Get the configuration properties for a specific topic.")
  getTopicConfigs(
    @path clusterName: string,
    @path topicName: string,
  ): TopicConfig[];

  @patch(#{ implicitOptionality: true })
  @route("/{topicName}/replications")
  @operationId("changeReplicationFactor")
  @summary("changeReplicationFactor")
  @doc("Change the replication factor of a topic.")
  changeReplicationFactor(
    @path clusterName: string,
    @path topicName: string,
    @body change: ReplicationFactorChange,
  ): ReplicationFactorChangeResponse | ApiNotFoundResponse | ApiBadRequestResponse;

  @get
  @route("/{topicName}/activeproducers")
  @operationId("getActiveProducerStates")
  @summary("getActiveProducerStates")
  @doc("Get active producer states for a specific topic.")
  getActiveProducerStates(
    @path clusterName: string,
    @path topicName: string,
  ): TopicProducerState[];

  @patch(#{ implicitOptionality: true })
  @route("/{topicName}/partitions")
  @summary("increaseTopicPartitions")
  @doc("Increase the number of partitions for a topic.")
  @operationId("increaseTopicPartitions")
  increaseTopicPartitions(
    @path clusterName: string,
    @path topicName: string,
    @body partitionsIncrease: PartitionsIncrease,
  ): PartitionsIncreaseResponse | ApiNotFoundResponse;

  @get
  @route("/{topicName}/connectors")
  @operationId("getTopicConnectors")
  @summary("getTopicConnectors")
  @doc("Get Kafka Connect connectors associated with a specific topic.")
  connectors(
    @path clusterName: string,
    @path topicName: string
  ) : FullConnectorInfo[];

  @get
  @route("/{topicName}/acls")
  @summary("listTopicAcls")
  @doc("List ACL entries for a specific topic.")
  @operationId("listTopicAcls")
  listTopicAcls(
    @path clusterName: string,
    @path topicName: string,
  ): KafkaAcl[];
}

model TopicsResponse {
  pageCount?: int32;
  topics?: Topic[];
}

enum TopicColumnsToSort {
  NAME,
  OUT_OF_SYNC_REPLICAS,
  TOTAL_PARTITIONS,
  REPLICATION_FACTOR,
  SIZE,
  MESSAGES_COUNT
}

model Topic {
  name: string;
  internal?: boolean;
  partitionCount?: int32;
  replicationFactor?: int32;
  replicas?: int32;
  inSyncReplicas?: int32;
  segmentSize?: int64;
  segmentCount?: int32;
  bytesInPerSec?: float64;
  bytesOutPerSec?: float64;
  underReplicatedPartitions?: int32;
  messagesCount?: int64 | null;
  cleanUpPolicy?: CleanUpPolicy;
  partitions?: Partition[];
}

model TopicUpdate {
  configs: Record<string>;
}

model TopicAnalysis {
  progress?: TopicAnalysisProgress;
  result?: TopicAnalysisResult;
}

model TopicAnalysisProgress {
  startedAt?: int64;
  completenessPercent?: decimal;
  msgsScanned?: int64;
  bytesScanned?: int64;
}

model TopicAnalysisResult {
  startedAt?: int64;
  finishedAt?: int64;
  error?: string;
  totalStats?: TopicAnalysisStats;
  partitionStats?: TopicAnalysisStats[];
}

model TopicAnalysisStats {
  @doc("null if this is total stats")
  partition?: int32;

  totalMsgs?: int64;
  minOffset?: int64;
  maxOffset?: int64;
  minTimestamp?: int64;
  maxTimestamp?: int64;
  nullKeys?: int64;
  nullValues?: int64;
  approxUniqKeys?: int64;
  approxUniqValues?: int64;
  keySize?: TopicAnalysisSizeStats;
  valueSize?: TopicAnalysisSizeStats;
  hourlyMsgCounts?: {
    hourStart?: int64;
    count?: int64;
  }[];
}

model TopicAnalysisSizeStats {
  sum?: int64;
  min?: int64;
  max?: int64;
  avg?: int64;
  prctl50?: int64;
  prctl75?: int64;
  prctl95?: int64;
  prctl99?: int64;
  prctl999?: int64;
}

model TopicProducerState {
  partition?: int32;
  producerId?: int64;
  producerEpoch?: int32;
  lastSequence?: int32;
  lastTimestampMs?: int64;
  coordinatorEpoch?: int32;
  currentTransactionStartOffset?: int64;
}

model TopicDetails {
  name: string;
  internal?: boolean;
  partitions?: Partition[];
  partitionCount?: int32;
  replicationFactor?: int32;
  replicas?: int32;
  inSyncReplicas?: int32;
  bytesInPerSec?: float64;
  bytesOutPerSec?: float64;
  segmentSize?: int64;
  segmentCount?: int32;
  underReplicatedPartitions?: int32;
  cleanUpPolicy?: CleanUpPolicy;
  keySerde?: string;
  valueSerde?: string;
}

model TopicConfig {
  name: string;
  value?: string;
  defaultValue?: string;
  source?: ConfigSource;
  isSensitive?: boolean;
  isReadOnly?: boolean;
  synonyms?: ConfigSynonym[];
  doc?: string;
}

model TopicCreation {
  name: string;
  partitions: int32;
  replicationFactor?: int32;
  configs?: Record<string>;
}

enum CleanUpPolicy {
  DELETE,
  COMPACT,
  COMPACT_DELETE,
  UNKNOWN,
}

model ConfigSynonym {
  name?: string;
  value?: string;
  source?: ConfigSource;
}

model Partition {
  partition: int32;
  leader?: int32;
  replicas?: Replica[];
  offsetMax: int64;
  offsetMin: int64;
}

model PartitionsIncrease {
  @minValue(1)
  totalPartitionsCount: integer;
}

model PartitionsIncreaseResponse {
  totalPartitionsCount: integer;
  topicName: string;
}


model ReplicationFactorChange {
  totalReplicationFactor: integer;
}

model ReplicationFactorChangeResponse {
  totalReplicationFactor: integer;
  topicName: string;
}

model Replica {
  broker?: int32;
  leader?: boolean;
  inSync?: boolean;
}
