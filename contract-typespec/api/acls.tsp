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
  @doc("Export ACLs as CSV.")
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
  @doc("Sync ACLs from a CSV definition.")
  @post
  @operationId("syncAclsCsv")
  syncAclsCsv(@path clusterName: string, @body content: string): void | ApiBadRequestResponse;

  @post
  @operationId("createAcl")
  @summary("createAcl")
  @doc("Create a new ACL entry in a Kafka cluster.")
  createAcl(@path clusterName: string, @body acl: KafkaAcl): void | ApiBadRequestResponse;

  @delete
  @operationId("deleteAcl")
  @summary("deleteAcl")
  @doc("Delete an ACL entry from a Kafka cluster.")
  deleteAcl(
    @path clusterName: string,
    @body acl: KafkaAcl,
  ): void | ApiNotFoundResponse;

  @route("/consumer")
  @post
  @operationId("createConsumerAcl")
  @summary("createConsumerAcl")
  @doc("Create ACL entries for a consumer.")
  createConsumerAcl(
    @path clusterName: string,
    @body payload: CreateConsumerAcl,
  ): void | ApiBadRequestResponse;

  @route("/producer")
  @summary("createProducerAcl")
  @doc("Create ACL entries for a producer.")
  @operationId("createProducerAcl")
  @post
  createProducerAcl(
    @path clusterName: string,
    @body payload: CreateProducerAcl,
  ): void | ApiBadRequestResponse;

  @route("/streamapp")
  @summary("createStreamAppAcl")
  @doc("Create ACL entries for a Kafka Streams application.")
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
