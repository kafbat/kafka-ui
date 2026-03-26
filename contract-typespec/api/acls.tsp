import "@typespec/openapi";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/acls")
@tag("Acls")
interface AclApi {
  @summary("listKafkaAcls")
  @get
  @operationId("listAcls")
  listAcls(
    @path clusterName: string,
    @query resourceType?: KafkaAclResourceType,
    @query resourceName?: string,
    @query namePatternType?: KafkaAclNamePatternType,
    @query search?: string,
    @query fts?: boolean
  ): KafkaAcl[];

  @route("/csv")
  @summary("getAclAsCsv")
  @get
  @operationId("getAclAsCsv")
  getAclAsCsv(
    @path clusterName: string,
    @query resourceType?: KafkaAclResourceType,
    @query resourceName?: string,
    @query namePatternType?: KafkaAclNamePatternType,
    @query search?: string,
    @query fts?: boolean
  ): CsvResponse;

  @route("/csv")
  @summary("syncAclsCsv")
  @post
  @operationId("syncAclsCsv")
  syncAclsCsv(@path clusterName: string, @body content: string): void | ApiBadRequestResponse;

  @post
  @operationId("createAcl")
  @summary("createAcl")
  createAcl(@path clusterName: string, @body acl: KafkaAcl): void | ApiBadRequestResponse;

  @summary("deleteAcl")
  @delete
  @operationId("deleteAcl")
  @summary("deleteAcl")
  deleteAcl(
    @path clusterName: string,
    @body acl: KafkaAcl,
  ): void | ApiNotFoundResponse;

  @route("/consumer")
  @post
  @operationId("createConsumerAcl")
  @summary("createConsumerAcl")
  createConsumerAcl(
    @path clusterName: string,
    @body payload: CreateConsumerAcl,
  ): void | ApiBadRequestResponse;

  @route("/producer")
  @summary("createProducerAcl")
  @operationId("createProducerAcl")
  @post
  createProducerAcl(
    @path clusterName: string,
    @body payload: CreateProducerAcl,
  ): void | ApiBadRequestResponse;

  @route("/streamapp")
  @summary("createStreamAppAcl")
  @post
  @operationId("createStreamAppAcl")
  createStreamAppAcl(
    @path clusterName: string,
    @body payload: CreateStreamAppAcl,
  ): void | ApiBadRequestResponse;
}

model KafkaAcl {
  resourceType: KafkaAclResourceType;
  resourceName: string; // "*" if acl can be applied to any resource of given type
  namePatternType: KafkaAclNamePatternType;
  principal: string;
  host: string;
  operation:  KafkaAclOpeations;
  permission: "ALLOW" | "DENY";
}

alias KafkaAclOpeations =
  "UNKNOWN"
  | "ALL"
  | "READ"
  | "WRITE"
  | "CREATE"
  | "DELETE"
  | "ALTER"
  | "DESCRIBE"
  | "CLUSTER_ACTION"
  | "DESCRIBE_CONFIGS"
  | "ALTER_CONFIGS"
  | "IDEMPOTENT_WRITE"
  | "CREATE_TOKENS"
  | "DESCRIBE_TOKENS";

enum KafkaAclResourceType {
  UNKNOWN,
  TOPIC,
  GROUP,
  CLUSTER,
  TRANSACTIONAL_ID,
  DELEGATION_TOKEN,
  USER,
}

enum KafkaAclNamePatternType {
  MATCH,
  LITERAL,
  PREFIXED,
}

model CreateConsumerAcl {
  principal?: string;
  host?: string;
  topics?: string[];
  topicsPrefix?: string;
  consumerGroups?: string[];
  consumerGroupsPrefix?: string;
}

model CreateProducerAcl {
  principal?: string;
  host?: string;
  topics?: string[];
  topicsPrefix?: string;
  transactionalId?: string;
  transactionsIdPrefix?: string;
  idempotent?: boolean = false;
}

model CreateStreamAppAcl {
  principal?: string;
  host?: string;
  inputTopics?: string[];
  outputTopics?: string[];
  applicationId?: string;
}
