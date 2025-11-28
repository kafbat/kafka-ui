import "@typespec/openapi";
import "./models.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/metrics")
@tag("PrometheusExpose")
interface PrometheusExposeApi {
    @get
    @summary("exposeAllMetrics")
    @operationId("exposeAllMetrics")
    exposeAllMetrics(): string;

    @get
    @route("/{clusterName}")
    @summary("exposeClusterMetrics")
    @operationId("exposeClusterMetrics")
    exposeClusterMetrics(@path clusterName: string): string;
}
