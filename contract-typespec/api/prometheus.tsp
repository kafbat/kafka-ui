import "@typespec/openapi";
import "./models.tsp";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/metrics")
@tag("PrometheusExpose")
interface PrometheusExposeApi {
    @get
    @summary("exposeAllMetrics")
    @operationId("exposeAllMetrics")
    // Return nothing (the controller handles the stream manually)
    exposeAllMetrics(): void;

    @get
    @route("/{clusterName}")
    @summary("exposeClusterMetrics")
    @operationId("exposeClusterMetrics")
    // Also return nothing
    exposeClusterMetrics(@path clusterName: string): void;
}
