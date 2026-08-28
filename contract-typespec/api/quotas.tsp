import "@typespec/openapi";
import "./models.tsp";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/clientquotas")
@tag("ClientQuotas")
interface QuoatsApi {
  @get
  @summary("listQuotas")
  @operationId("listQuotas")
  @doc("List all client quotas configured in a Kafka cluster.")
  listQuotas(@path clusterName: string): ClientQuotas[];

  @post
  @summary("upsertClientQuotas")
  @operationId("upsertClientQuotas")
  @doc("Create or update client quotas in a Kafka cluster.")
  upsertClientQuotas(
    @path clusterName: string,
    @body quota: ClientQuotas,
  ): void | ApiBadRequestResponse;
}

model ClientQuotas {
  user?: string;
  clientId?: string;
  ip?: string;
  quotas?: Record<decimal>;
}
