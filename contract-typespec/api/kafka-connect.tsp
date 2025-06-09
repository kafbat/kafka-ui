import "@typespec/openapi";
import "./responses.tsp";

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters/{clusterName}/connects")
@tag("Kafka Connect")
interface ConnectInstancesApi {
  @get
  @operationId("getConnects")
  getConnects(@path clusterName: string): Connect[];

  @get
  @route("/{connectName}/plugins")
  @doc("get connector plugins")
  @operationId("getConnectorPlugins")
  getConnectorPlugins(
    @path clusterName: string,
    @path connectName: string,
  ): ConnectorPlugin[];

  @put
  @route("/{connectName}/plugins/{pluginName}/config/validate")
  @doc("validate connector plugin configuration")
  @operationId("validateConnectorPluginConfig")
  validateConnectorPluginConfig(
    @path clusterName: string,
    @path connectName: string,
    @path pluginName: string,
    @body config: ConnectorConfig,
  ): ConnectorPluginConfigValidationResponse;
}

// /api/clusters/{clusterName}/connectors
@route("/api/clusters/{clusterName}/connectors")
@tag("Kafka Connect")
interface ConnectorsApi {
  @get
  @operationId("getAllConnectors")
  getAllConnectors(
    @path clusterName: string,
    @query search?: string,
    @query orderBy?: ConnectorColumnsToSort,
    @query sortOrder?: SortOrder,
  ): FullConnectorInfo[];
}

// /api/clusters/{clusterName}/connects/{connectName}/connectors
@route("/api/clusters/{clusterName}/connects/{connectName}/connectors")
@tag("Kafka Connect")
interface KafkaConnectConnectorsApi {
  @get
  @operationId("getConnectors")
  getConnectors(@path clusterName: string, @path connectName: string): string[];

  @post
  @operationId("createConnector")
  createConnector(
    @path clusterName: string,
    @path connectName: string,
    @body connector: NewConnector,
  ): Connector | ApiRebalanceInProgressResponse;

  @get
  @route("/{connectorName}")
  @operationId("getConnector")
  getConnector(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
  ): Connector;

  @delete
  @route("/{connectorName}")
  @operationId("deleteConnector")
  deleteConnector(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
  ): void | ApiRebalanceInProgressResponse;

  @post
  @route("/{connectorName}/action/{action}")
  @operationId("updateConnectorState")
  updateConnectorState(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
    @path action: ConnectorAction,
  ): void | ApiRebalanceInProgressResponse | ApiBadRequestResponse;

  @get
  @route("/{connectorName}/config")
  @operationId("getConnectorConfig")
  getConnectorConfig(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
  ): ConnectorConfig;

  @put
  @route("/{connectorName}/config")
  @operationId("setConnectorConfig")
  setConnectorConfig(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
    @body config: ConnectorConfig,
  ): Connector | ApiRebalanceInProgressResponse | ApiBadRequestResponse;

  @get
  @route("/{connectorName}/tasks")
  @operationId("getConnectorTasks")
  getConnectorTasks(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
  ): Task[];

  @post
  @route("/{connectorName}/tasks/{taskId}/action/restart")
  @operationId("restartConnectorTask")
  restartConnectorTask(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
    @path taskId: int32,
  ): void | ApiBadRequestResponse;

  @delete
  @route("/{connectorName}/offsets")
  @operationId("resetConnectorOffsets")
  resetConnectorOffsets(
    @path clusterName: string,
    @path connectName: string,
    @path connectorName: string,
  ): void | ApiBadRequestResponse;
}


model Connect {
  name: string;
  address?: string;
}

model ConnectorConfig is Record<unknown>;

model TaskId {
  connector?: string;
  task?: int32;
}

model TaskStatus {
  id: int32;
  state: ConnectorTaskStatus;
  worker_id: string;
  trace?: string;
}

model Task {
  id?: TaskId;
  status: TaskStatus;
  config?: ConnectorConfig;
}

model NewConnector {
  name: string;
  config: ConnectorConfig;
}

enum ConnectorType {
  SOURCE,
  SINK,
}

enum ConnectorTaskStatus {
  RUNNING,
  FAILED,
  PAUSED,
  RESTARTING,
  UNASSIGNED,
}

enum ConnectorState {
  RUNNING,
  FAILED,
  PAUSED,
  UNASSIGNED,
  TASK_FAILED,
  RESTARTING,
  STOPPED,
}

model ConnectorStatus {
  state: ConnectorState;
  worker_id: string;
}

model Connector extends NewConnector {
  tasks?: TaskId[];
  type: ConnectorType;
  status: ConnectorStatus;
  connect: string;
}

enum ConnectorAction {
  RESTART,
  RESTART_ALL_TASKS,
  RESTART_FAILED_TASKS,
  PAUSE,
  RESUME,
  STOP,
}

enum TaskAction {
  restart,
}

model ConnectorPlugin {
  class?: string;
}

model ConnectorPluginConfigDefinition {
  name?: string;
  type?:
    | "BOOLEAN"
    | "CLASS"
    | "DOUBLE"
    | "INT"
    | "LIST"
    | "LONG"
    | "PASSWORD"
    | "SHORT"
    | "STRING";
  required?: boolean;
  default_value?: string;
  importance?: "LOW" | "MEDIUM" | "HIGH";
  documentation?: string;
  group?: string;
  width?: "SHORT" | "MEDIUM" | "LONG" | "NONE";
  display_name?: string;
  dependents?: string[];
  order?: int32;
}

model ConnectorPluginConfigValue {
  name?: string;
  value?: string;
  recommended_values?: string[];
  errors?: string[];
  visible?: boolean;
}

model ConnectorPluginConfig {
  definition?: ConnectorPluginConfigDefinition;
  value?: ConnectorPluginConfigValue;
}

model ConnectorPluginConfigValidationResponse {
  name?: string;
  error_count?: int32;
  groups?: string[];
  configs?: ConnectorPluginConfig[];
}

model FullConnectorInfo {
  connect: string;
  name: string;
  connector_class?: string;
  type?: ConnectorType;
  topics?: string[];
  status: ConnectorStatus;
  tasks_count?: integer;
  failed_tasks_count?: integer;
}

enum ConnectorColumnsToSort {
  NAME,
  CONNECT,
  TYPE,
  STATUS,
}
