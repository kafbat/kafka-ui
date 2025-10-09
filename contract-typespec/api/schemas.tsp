import "@typespec/openapi";
import "./responses.tsp";
import "./models.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/schemas")
@tag("Schemas")
interface SchemasApi {
  @post
  @operationId("createNewSchema")
  @summary("createNewSchema")
  createNewSchema(@path clusterName: string, @body input: NewSchemaSubject):
    | SchemaSubject
    | ApiBadRequestResponse
    | ApiDuplicateResponse
    | ApiInvalidParametersResponse;

  @get
  @operationId("getSchemas")
  @summary("getSchemas")
  getSchemas(
    @path clusterName: string,
    @query page?: int32,
    @query perPage?: int32,
    @query search?: string,
    @query orderBy?: SchemaColumnsToSort,
    @query sortOrder?: SortOrder,
    @query fts?: boolean
  ): SchemaSubjectsResponse;

  @delete
  @route("/{subject}")
  @operationId("deleteSchema")
  @summary("deleteSchema")
  deleteSchema(
    @path clusterName: string,
    @path subject: string,
  ): void | ApiNotFoundResponse;

  @get
  @route("/{subject}/versions")
  @operationId("getAllVersionsBySubject")
  @summary("getAllVersionsBySubject")
  getAllVersionsBySubject(
    @path clusterName: string,
    @path subject: string,
  ): SchemaSubject[];

  @get
  @route("/{subject}/latest")
  @operationId("getLatestSchema")
  @summary("getLatestSchema")
  getLatestSchema(
    @path clusterName: string,
    @path subject: string,
  ): SchemaSubject;

  @delete
  @route("/{subject}/latest")
  @operationId("deleteLatestSchema")
  @summary("deleteLatestSchema")
  deleteLatestSchema(
    @path clusterName: string,
    @path subject: string,
  ): void | ApiNotFoundResponse;

  @get
  @route("/{subject}/versions/{version}")
  @operationId("getSchemaByVersion")
  @summary("getSchemaByVersion")
  getSchemaByVersion(
    @path clusterName: string,
    @path subject: string,
    @path version: int32,
  ): SchemaSubject;

  @delete
  @route("/{subject}/versions/{version}")
  @operationId("deleteSchemaByVersion")
  @summary("deleteSchemaByVersion")
  deleteSchemaByVersion(
    @path clusterName: string,
    @path subject: string,
    @path version: int32,
  ): void | ApiNotFoundResponse;

  @get
  @route("/compatibility")
  @operationId("getGlobalSchemaCompatibilityLevel")
  @summary("getGlobalSchemaCompatibilityLevel")
  getGlobalSchemaCompatibilityLevel(
    @path clusterName: string,
  ): CompatibilityLevel;

  @put
  @route("/compatibility")
  @operationId("updateGlobalSchemaCompatibilityLevel")
  @summary("updateGlobalSchemaCompatibilityLevel")
  updateGlobalSchemaCompatibilityLevel(
    @path clusterName: string,
    @body level: CompatibilityLevel,
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @put
  @route("/{subject}/compatibility")
  @operationId("updateSchemaCompatibilityLevel")
  @summary("updateSchemaCompatibilityLevel")
  updateSchemaCompatibilityLevel(
    @path clusterName: string,
    @path subject: string,
    @body level: CompatibilityLevel,
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @post
  @route("/{subject}/check")
  @operationId("checkSchemaCompatibility")
  @summary("checkSchemaCompatibility")
  checkSchemaCompatibility(
    @path clusterName: string,
    @path subject: string,
    @body input: NewSchemaSubject,
  ): CompatibilityCheckResponse | ApiNotFoundResponse;
}

model SchemaReference {
  name: string;
  subject: string;
  version: int32;
}

enum SchemaType {
  AVRO,
  JSON,
  PROTOBUF,
}

model SchemaSubject {
  id: int32;
  subject: string;
  version: string;
  schema: string;
  compatibilityLevel: string;
  schemaType: SchemaType;
  references?: SchemaReference[];
}

model NewSchemaSubject {
  @doc("should be set for creating/updating schema subject")
  subject: string;

  schema: string;
  schemaType: SchemaType;
  references?: SchemaReference[];
}

model CompatibilityLevel {
  compatibility:
    | "BACKWARD"
    | "BACKWARD_TRANSITIVE"
    | "FORWARD"
    | "FORWARD_TRANSITIVE"
    | "FULL"
    | "FULL_TRANSITIVE"
    | "NONE";
}

model CompatibilityCheckResponse {
  isCompatible: boolean;
}

model SchemaSubjectsResponse {
  pageCount?: int32;
  schemas?: SchemaSubject[];
}

enum SchemaColumnsToSort {
  SUBJECT,
  ID,
  TYPE,
  COMPATIBILITY,
  VERSION
}
