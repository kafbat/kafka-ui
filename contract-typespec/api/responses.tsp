import "@typespec/http";
import "@typespec/http/streams";

namespace Api;

using TypeSpec.Http;
using TypeSpec.Streams;

model ApiCreatedResponse<Model> is Response<201> {
  @body
  body: Model;
}

model SseResponse<Model> is Stream<Model> {
  @header contentType: "text/event-stream";
  @body body: Model[];
}

// ----- Error Responses -----
model ApiNotFoundResponse is Response<404> {
  message: string;
}

model ApiTimeoutResponse is Response<408> {
  message: string;
}

model ApiDuplicateResponse is Response<409> {
  message: string;
}

model ApiRebalanceInProgressResponse is Response<409> {
  message: string;
}

model ApiInvalidParametersResponse is Response<422> {
  message: string;
}

model ApiBadRequestResponse is Response<400> {
  @body
  message: ErrorResponse;
}

model ApiUnauthorized is Response<401> {
  message: string;
}

model ErrorResponse {
  @doc("Internal error code (can be used for message formatting & localization on UI)")
  code: int32;

  @doc("Error message")
  message: string;

  @doc("Response unix timestamp in ms")
  timestamp: decimal;

  @doc("Unique server-defined request id for convenient debugging")
  requestId: string;

  fieldsErrors?: FieldError[];
  stackTrace?: string;
}

model FieldError {
  @doc("Name of field that violated format")
  fieldName: string;

  @doc("Field format violations description (ex. [\"size must be between 0 and 20\", \"must be a well-formed email address\"])")
  restrictions?: string[];
}

model CsvResponse is Response<200> {
    @header contentType: "text/csv";
    @body _: string;
}
