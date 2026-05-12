import "@typespec/openapi";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/graphs")
@tag("Graphs")
interface GraphsApi {
    @get
    @operationId("getGraphsList")
    @summary("getGraphsList")
    getGraphsList(
        @path clusterName: string
    ): GraphDescriptions;

    @post
    @operationId("getGraphData")
    @summary("getGraphData")
    getGraphData(
        @path clusterName: string,
        @body request: GraphDataRequest
    ) : PrometheusApiQueryResponse
}

model GraphDescriptions {
    graphs?: Array<GraphDescription>;
}

model GraphDescription {
    @doc("Id that should be used to query data on API level")
    id: string;
    type?: "range" | "instant";
    @doc("""
        ISO_8601 duration string (for "range" graphs only)
        """)
    defaultPeriod?: string;
    parameters?: Array<GraphParameter>;
}

model GraphParameter {
    name: string;
}

model GraphDataRequest {
    id?: string;
    parameters?: Record<string>;
    from?: offsetDateTime;
    to?: offsetDateTime;
}

model PrometheusApiBaseResponse {
    status: "success" | "error";
    error?: string;
    errorType?: string;
    warnings?: Array<string>;
}

model PrometheusApiQueryResponse extends PrometheusApiBaseResponse {
    data?: PrometheusApiQueryResponseData;
}

model PrometheusApiQueryResponseData {
    resultType: "matrix" | "vector" | "scalar" | "string";
    @doc("""
            Depending on resultType format can vary:
            "vector":
              [
                {
                  "metric": { "<label_name>": "<label_value>", ... },
                  "value": [ <unix_time>, "<sample_value>" ],
                  "histogram": [ <unix_time>, <histogram> ]
               }, ...
              ]
            "matrix":
              [
                {
                  "metric": { "<label_name>": "<label_value>", ... },
                  "values": [ [ <unix_time>, "<sample_value>" ], ... ],
                  "histograms": [ [ <unix_time>, <histogram> ], ... ]
                }, ...
              ]
            "scalar":
              [ <unix_time>, "<scalar_value>" ]
            "string":
              [ <unix_time>, "<string_value>" ]
    """)
    result?: Array<unknown>;
}
