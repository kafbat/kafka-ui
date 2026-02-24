import "@typespec/openapi";
import "./models.tsp";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/topics/{topicName}")
@tag("Messages")
interface MessagesApi {
  @get
  @route("/serdes")
  @operationId("getSerdes")
  @summary("getSerdes")
  getSerdes(
    @path clusterName: string,
    @path topicName: string,
    @query use: SerdeUsage,
  ): TopicSerdeSuggestion;

  @get
  @route("/messages")
  @operationId("getTopicMessages")
  @summary("getTopicMessages")
  getTopicMessages(
    @path clusterName: string,
    @path topicName: string,
    @query seekType?: SeekType,
    @query seekTo?: string[],
    @query limit?: int32,
    @query q?: string,
    @query filterQueryType?: MessageFilterType,
    @query seekDirection?: SeekDirection,
    @query keySerde?: string,
    @query valueSerde?: string,
  ): SseResponse<TopicMessageEvent>;

  @delete
  @route("/messages")
  @operationId("deleteTopicMessages")
  @summary("deleteTopicMessages")
  deleteTopicMessages(
    @path clusterName: string,
    @path topicName: string,
    @query partitions?: int32[],
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @post
  @route("/messages")
  @operationId("sendTopicMessages")
  @summary("sendTopicMessages")
  sendTopicMessages(
    @path clusterName: string,
    @path topicName: string,
    @body message: CreateTopicMessage,
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @post
  @route("/smartfilters")
  @operationId("registerFilter")
  @summary("registerFilter")
  registerFilter(
    @path clusterName: string,
    @path topicName: string,
    @body registration: MessageFilterRegistration,
  ): MessageFilterId | ApiBadRequestResponse;

  @get
  @route("/messages/v2")
  @operationId("getTopicMessagesV2")
  @summary("getTopicMessagesV2")
  getTopicMessagesV2(
    @path clusterName: string,
    @path topicName: string,
    @query mode?: PollingMode,
    @query partitions?: int32[],
    @query limit?: int32,
    @query stringFilter?: string,
    @query smartFilterId?: string,
    @query offset?: int64,
    @query timestamp?: int64,
    @query endTimestamp?: int64,
    @query keySerde?: string,
    @query valueSerde?: string,
    @query cursor?: string,
  ): SseResponse<TopicMessageEvent> | ApiBadRequestResponse;
}

@route("/api/smartfilters/testexecutions")
@tag("Messages")
interface SmartFiltersTestExecutionsApi {
  @put
  @operationId("executeSmartFilterTest")
  @summary("executeSmartFilterTest")
  executeSmartFilterTest(
    @body input: SmartFilterTestExecution,
  ): SmartFilterTestExecutionResult | ApiBadRequestResponse;
}

model TopicSerdeSuggestion {
  key?: SerdeDescription[];
  value?: SerdeDescription[];
}

enum SerdeUsage {
  SERIALIZE,
  DESERIALIZE,
}

model TopicMessageEvent {
  type?: "PHASE" | "MESSAGE" | "CONSUMING" | "DONE";
  message?: TopicMessage;
  phase?: TopicMessagePhase;
  consuming?: TopicMessageConsuming;
  cursor?: TopicMessageNextPageCursor;
}

model TopicMessagePhase {
  name?: string;
}

model TimeStampFormat {
  timeStampFormat?: string;
}

model TopicMessageConsuming {
  bytesConsumed?: int64;
  elapsedMs?: int64;
  isCancelled?: boolean;
  messagesConsumed?: int32;
  filterApplyErrors?: int32;
}

model TopicMessageNextPageCursor {
  id?: string;
}

model TopicMessage {
  partition: int32;
  offset: int64;
  timestamp: offsetDateTime;
  timestampType?: "NO_TIMESTAMP_TYPE" | "CREATE_TIME" | "LOG_APPEND_TIME";
  key?: string;
  headers?: Record<string>;
  value?: string;
  #deprecated "use 'keySerde' field instead"
  keyFormat?: MessageFormat;
  #deprecated "use 'valueSerde' field instead"
  valueFormat?: MessageFormat;
  keySize?: int64;
  valueSize?: int64;
  #deprecated "use 'keyDeserializeProperties' field instead"
  keySchemaId?: string;
  #deprecated "use 'valueDeserializeProperties' field instead"
  valueSchemaId?: string;
  headersSize?: int64;
  keySerde?: string;
  valueSerde?: string;
  keyDeserializeProperties?: Record<unknown>;
  valueDeserializeProperties?: Record<unknown>;
}

model SchemaRegistryDeserializeProperties {
    type: string;
    id: int32;
    subjects?: string[];
}

enum SeekType {
  BEGINNING,
  OFFSET,
  TIMESTAMP,
  LATEST,
}

model MessageFilterRegistration {
  filterCode?: string;
}

model MessageFilterId {
  id?: string;
}

enum PollingMode {
  FROM_OFFSET,
  TO_OFFSET,
  FROM_TIMESTAMP,
  TO_TIMESTAMP,
  TIMESTAMP_RANGE,
  LATEST,
  EARLIEST,
  TAILING,
}

enum MessageFilterType {
  STRING_CONTAINS,
  CEL_SCRIPT,
}

enum SeekDirection {
  FORWARD,
  BACKWARD,
  TAILING,
}

model SmartFilterTestExecution {
  filterCode: string;
  key?: string;
  value?: string;
  headers?: Record<string>;
  partition?: int32;
  offset?: int64;
  timestampMs?: int64;
}

model SmartFilterTestExecutionResult {
  result?: boolean;
  error?: string;
}

model CreateTopicMessage {
  partition: int32;
  key?: string | null;
  headers?: Record<string>;
  value?: string | null;
  keySerde?: string | null;
  valueSerde?: string | null;
}

enum MessageFormat {
  AVRO,
  JSON,
  PROTOBUF,
  UNKNOWN,
}


model SerdeDescription {
  name?: string;
  description?: string;

  @doc("This serde was automatically chosen by cluster config. This should be enabled in UI by default. Also it will be used for deserialization if no serdes passed.")
  preferred?: boolean;

  schema?: string;
  additionalProperties?: Record<unknown>;
}
