import "@typespec/openapi";
import "./responses.tsp";

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/ksql")
@tag("Ksql")
interface KsqlApi {
  @post
  @route("/v2")
  @operationId("executeKsql")
  executeKsql(
    @path clusterName: string,
    @body command: KsqlCommandV2,
  ): KsqlCommandV2Response | ApiBadRequestResponse;

  @get
  @route("/tables")
  @operationId("listTables")
  listTables(@path clusterName: string): KsqlTableDescription[];

  @get
  @route("/streams")
  @operationId("listStreams")
  listStreams(@path clusterName: string): KsqlStreamDescription[];

  @get
  @route("/response")
  @operationId("openKsqlResponsePipe")
  openKsqlResponsePipe(
    @path clusterName: string,
    @query pipeId: string,
  ): SseResponse<KsqlResponse>;
}


model KsqlCommandV2 {
  ksql: string;
  streamsProperties?: Record<string>;
}

model KsqlCommandV2Response {
  pipeId: string;
}

model KsqlTableDescription {
  name?: string;
  topic?: string;
  keyFormat?: string;
  valueFormat?: string;
  isWindowed?: boolean;
}

model KsqlStreamDescription {
  name?: string;
  topic?: string;
  keyFormat?: string;
  valueFormat?: string;
}

model KsqlTableResponse {
  header?: string;
  columnNames?: string[];
  values?: unknown[][];
}

model KsqlResponse {
  table?: KsqlTableResponse;
}