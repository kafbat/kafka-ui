import "@typespec/openapi";

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/acls")
@tag("Acls")
interface AclApi {
  @doc("listKafkaAcls")
  @get
  @operationId("listAcls")
  listAcls(
    @path clusterName: string,
    @query resourceType?: KafkaAclResourceType,
    @query resourceName?: string,
    @query namePatternType?: KafkaAclNamePatternType,
  ): KafkaAcl[];

  @route("/csv")
  @doc("getAclAsCsv")
  @get
  @operationId("getAclAsCsv")
  getAclAsCsv(@path clusterName: string): string;

  @route("/csv")
  @doc("syncAclsCsv")
  @post
  @operationId("syncAclsCsv")
  syncAclsCsv(@path clusterName: string, @body content: string): void | ApiBadRequestResponse;

  @doc("createAcl")
  @post
  @operationId("createAcl")
  createAcl(@path clusterName: string, @body acl: KafkaAcl): void | ApiBadRequestResponse;

  @doc("deleteAcl")
  @delete
  @operationId("deleteAcl")
  deleteAcl(
    @path clusterName: string,
    @body acl: KafkaAcl,
  ): void | ApiNotFoundResponse;

  @route("/consumer")
  @doc("createConsumerAcl")
  @post
  @operationId("createConsumerAcl")
  createConsumerAcl(
    @path clusterName: string,
    @body payload: CreateConsumerAcl,
  ): void | ApiBadRequestResponse;

  @route("/producer")
  @doc("createProducerAcl")
  @operationId("createProducerAcl")
  @post
  createProducerAcl(
    @path clusterName: string,
    @body payload: CreateProducerAcl,
  ): void | ApiBadRequestResponse;

  @route("/streamApp")
  @doc("createStreamAppAcl")
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
  operation:  "UNKNOWN" | "ALL" | "READ" | "WRITE" | "CREATE" | "DELETE" | "ALTER" | "DESCRIBE" | "CLUSTER_ACTION" | "DESCRIBE_CONFIGS" | "ALTER_CONFIGS" | "IDEMPOTENT_WRITE" | "CREATE_TOKENS" | "DESCRIBE_TOKENS";
  permission: "ALLOW" | "DENY";
}

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
  principal: string;
  host: string;
  topics?: string[];
  topicsPrefix?: string;
  consumerGroups?: string[];
  consumerGroupsPrefix?: string;
}

model CreateProducerAcl {
  principal: string;
  host: string;
  topics?: string[];
  topicsPrefix?: string;
  transactionalId?: string;
  transactionsIdPrefix?: string;
  idempotent?: boolean = false;
}

model CreateStreamAppAcl {
  principal: string;
  host: string;
  inputTopics: string[];
  outputTopics: string[];
  applicationId: string;
}