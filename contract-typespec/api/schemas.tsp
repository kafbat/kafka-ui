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
  @doc("Create a new schema subject or update an existing schema in the Schema Registry.")
  createNewSchema(@path clusterName: string, @body input: NewSchemaSubject):
    | SchemaSubject
    | ApiBadRequestResponse
    | ApiDuplicateResponse
    | ApiInvalidParametersResponse;

  @get
  @operationId("getSchemas")
  @summary("getSchemas")
  @doc("List all schema subjects from the Schema Registry.")
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
  @doc("Delete a schema subject from the Schema Registry.")
  deleteSchema(
    @path clusterName: string,
    @path subject: string,
  ): void | ApiNotFoundResponse;

  @get
  @route("/{subject}/versions")
  @operationId("getAllVersionsBySubject")
  @summary("getAllVersionsBySubject")
  @doc("Get all versions of a schema subject from the Schema Registry.")
  getAllVersionsBySubject(
    @path clusterName: string,
    @path subject: string,
  ): SchemaSubject[];

  @get
  @route("/{subject}/latest")
  @operationId("getLatestSchema")
  @summary("getLatestSchema")
  @doc("Get the latest version of a schema subject from the Schema Registry.")
  getLatestSchema(
    @path clusterName: string,
    @path subject: string,
  ): SchemaSubject;

  @delete
  @route("/{subject}/latest")
  @operationId("deleteLatestSchema")
  @summary("deleteLatestSchema")
  @doc("Delete the latest version of a schema subject from the Schema Registry.")
  deleteLatestSchema(
    @path clusterName: string,
    @path subject: string,
  ): void | ApiNotFoundResponse;

  @get
  @route("/{subject}/versions/{version}")
  @operationId("getSchemaByVersion")
  @summary("getSchemaByVersion")
  @doc("Get a specific version of a schema subject from the Schema Registry.")
  getSchemaByVersion(
    @path clusterName: string,
    @path subject: string,
    @path version: int32,
  ): SchemaSubject;

  @delete
  @route("/{subject}/versions/{version}")
  @operationId("deleteSchemaByVersion")
  @summary("deleteSchemaByVersion")
  @doc("Delete a specific version of a schema subject from the Schema Registry.")
  deleteSchemaByVersion(
    @path clusterName: string,
    @path subject: string,
    @path version: int32,
  ): void | ApiNotFoundResponse;

  @get
  @route("/compatibility")
  @operationId("getGlobalSchemaCompatibilityLevel")
  @summary("getGlobalSchemaCompatibilityLevel")
  @doc("Get the global schema compatibility level from the Schema Registry.")
  getGlobalSchemaCompatibilityLevel(
    @path clusterName: string,
  ): CompatibilityLevel;

  @put
  @route("/compatibility")
  @operationId("updateGlobalSchemaCompatibilityLevel")
  @summary("updateGlobalSchemaCompatibilityLevel")
  @doc("Update the global schema compatibility level in the Schema Registry.")
  updateGlobalSchemaCompatibilityLevel(
    @path clusterName: string,
    @body level: CompatibilityLevel,
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @put
  @route("/{subject}/compatibility")
  @operationId("updateSchemaCompatibilityLevel")
  @summary("updateSchemaCompatibilityLevel")
  @doc("Update the compatibility level for a specific schema subject.")
  updateSchemaCompatibilityLevel(
    @path clusterName: string,
    @path subject: string,
    @body level: CompatibilityLevel,
  ): void | ApiNotFoundResponse | ApiBadRequestResponse;

  @post
  @route("/{subject}/check")
  @operationId("checkSchemaCompatibility")
  @summary("checkSchemaCompatibility")
  @doc("Check if a schema is compatible with an existing subject version.")
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
  topic?: string | null;
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
