import "@typespec/openapi";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/consumer-groups")
@tag("Consumer Groups")
interface ConsumerGroupsApi {
  @get
  @route("/paged")
  @operationId("getConsumerGroupsPage")
  @summary("getConsumerGroupsPage")
  @doc("List consumer groups in a Kafka cluster with pagination and search support.")
  getConsumerGroupsPage(
    @path clusterName: string,
    @query page?: int32,
    @query perPage?: int32,
    @query search?: string,
    @query orderBy?: ConsumerGroupOrdering,
    @query sortOrder?: SortOrder,
    @query fts?: boolean,
    @query state?: ConsumerGroupState[]
  ): ConsumerGroupsPageResponse;

  @get
  @route("/lag")
  @operationId("getConsumerGroupsLag")
  @summary("getConsumerGroupsLag")
  @doc("Get lag information for consumer groups.")
  getConsumerGroupsLag(
    @path clusterName: string,
    @query ids: Array<string>,
    @query lastUpdate?: int64,
    @query includePartitions?: boolean
  ): ConsumerGroupsLagResponse;

  @get
  @route("/csv")
  @operationId("getConsumerGroupsCsv")
  @summary("getConsumerGroupsPage")
  @doc("Export consumer groups data as CSV.")
  getConsumerGroupsCsv(
    @path clusterName: string,
    @query page?: int32,
    @query perPage?: int32,
    @query search?: string,
    @query orderBy?: ConsumerGroupOrdering,
    @query sortOrder?: SortOrder,
    @query fts?: boolean,
    @query state?: ConsumerGroupState[]
  ): CsvResponse;

  @get
  @route("/{id}")
  @operationId("getConsumerGroup")
  @summary("getConsumerGroup")
  @doc("Get detailed information about a specific consumer group including member assignments and lag.")
  getConsumerGroup(
    @path clusterName: string,
    @path id: string,
  ): ConsumerGroupDetails;

  @delete
  @route("/{id}")
  @operationId("deleteConsumerGroup")
  @summary("deleteConsumerGroup")
  @doc("Delete a consumer group.")
  deleteConsumerGroup(@path clusterName: string, @path id: string): void;

  @post
  @route("/{id}/offsets")
  @operationId("resetConsumerGroupOffsets")
  @summary("resetConsumerGroupOffsets")
  @doc("Reset consumer group offsets to a specific position.")
  resetConsumerGroupOffsets(
    @path clusterName: string,
    @path id: string,
    @body reset: ConsumerGroupOffsetsReset,
  ): void;

  @delete
  @route("/{id}/topics/{topicName}")
  @operationId("deleteConsumerGroupOffsets")
  @summary("deleteConsumerGroupOffsets")
  @doc("Delete committed offsets for specific topic partitions in a consumer group.")
  deleteConsumerGroupOffsets(
    @path clusterName: string,
    @path id: string,
    @path topicName: string,
  ): void;
}

@route("/api/clusters/{clusterName}/topics/{topicName}/consumer-groups")
@tag("Consumer Groups")
interface TopicConsumerGroupsApi {
  @get
  @operationId("getTopicConsumerGroups")
  @summary("getTopicConsumerGroups")
  @doc("Get consumer groups that are consuming from a specific topic.")
  getTopicConsumerGroups(
    @path clusterName: string,
    @path topicName: string,
  ): ConsumerGroup[];
}

enum ConsumerGroupState {
  UNKNOWN,
  PREPARING_REBALANCE,
  COMPLETING_REBALANCE,
  STABLE,
  DEAD,
  EMPTY,
}

@discriminator("inherit")
model ConsumerGroup {
  @invisible(Lifecycle)
  inherit: string;
  groupId: string;
  members?: int32;
  topics?: int32;
  simple?: boolean;
  partitionAssignor?: string;
  state?: ConsumerGroupState;
  coordinator?: Broker;

  @doc("null if consumer group has no offsets committed")
  consumerLag?: int64;
}


model ConsumerGroupDetails extends ConsumerGroup {
  @invisible(Lifecycle)
  inherit: "details";
  partitions?: ConsumerGroupTopicPartition[];
}

enum ConsumerGroupOrdering {
  NAME,
  MEMBERS,
  STATE,
  MESSAGES_BEHIND,
  TOPIC_NUM,
}

model ConsumerGroupsPageResponse {
  pageCount?: int32;
  consumerGroups?: ConsumerGroup[];
}

model ConsumerGroupOffsetsReset {
  topic: string;
  resetType: ConsumerGroupOffsetsResetType;
  partitions?: int32[];
  resetToTimestamp?: int64;
  partitionsOffsets?: PartitionOffset[];
}

model ConsumerGroupsLagResponse {
    updateTimestamp: int64;
    consumerGroups: Record<ConsumerGroupLag>;
}

model ConsumerGroupLag {
    lag: int64;
    topics: Record<int64>;
    topicPartitions?: Record<ConsumerGroupTopicLag>;
}

model ConsumerGroupTopicLag {
    partitions?: Record<int64>;
}

enum ConsumerGroupOffsetsResetType {
  EARLIEST,
  LATEST,
  TIMESTAMP,
  OFFSET,
}

model ConsumerGroupTopicPartition {
  topic: string;
  partition: int32;
  currentOffset?: int64;
  endOffset?: int64;
  consumerLag?: int64; // null if consumer group has no offsets committed
  consumerId?: string;
  host?: string;
}

model PartitionOffset {
  partition: int32;
  offset?: int64;
}

