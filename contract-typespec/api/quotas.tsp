import "@typespec/openapi";
import "./models.tsp";
import "./responses.tsp";

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/clientquotas")
@tag("ClientQuotas")
interface QuoatsApi {
  @get
  @doc("listQuotas")
  @operationId("listQuotas")
  listQuotas(@path clusterName: string): ClientQuotas[];

  @post
  @doc("upsertClientQuotas")
  @operationId("upsertClientQuotas")
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
