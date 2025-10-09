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
  getConsumerGroupsPage(
    @path clusterName: string,
    @query page?: int32,
    @query perPage?: int32,
    @query search?: string,
    @query orderBy?: ConsumerGroupOrdering,
    @query sortOrder?: SortOrder,
    @query fts?: boolean
  ): ConsumerGroupsPageResponse;

  @get
  @route("/{id}")
  @operationId("getConsumerGroup")
  @summary("getConsumerGroup")
  getConsumerGroup(
    @path clusterName: string,
    @path id: string,
  ): ConsumerGroupDetails;

  @delete
  @route("/{id}")
  @operationId("deleteConsumerGroup")
  @summary("deleteConsumerGroup")
  deleteConsumerGroup(@path clusterName: string, @path id: string): void;

  @post
  @route("/{id}/offsets")
  @operationId("resetConsumerGroupOffsets")
  @summary("resetConsumerGroupOffsets")
  resetConsumerGroupOffsets(
    @path clusterName: string,
    @path id: string,
    @body reset: ConsumerGroupOffsetsReset,
  ): void;

  @delete
  @route("/{id}/topics/{topicName}")
  @operationId("deleteConsumerGroupOffsets")
  @summary("deleteConsumerGroupOffsets")
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

