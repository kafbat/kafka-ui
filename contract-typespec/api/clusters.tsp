import "@typespec/openapi";
import "./responses.tsp";

namespace Api;

using TypeSpec.Http;
using OpenAPI;

@route("/api/clusters")
@tag("Clusters")
interface ClustersApi {
  @get
  @operationId("getClusters")
  @summary("getClusters")
  getClusters(): Cluster[];

  @post
  @route("/{clusterName}/cache")
  @operationId("updateClusterInfo")
  @summary("updateClusterInfo")
  updateClusterInfo(@path clusterName: string): Cluster;

  @get
  @route("/{clusterName}/metrics")
  @operationId("getClusterMetrics")
  @summary("getClusterMetrics")
  getClusterMetrics(@path clusterName: string): ClusterMetrics;

  @get
  @route("/{clusterName}/stats")
  @operationId("getClusterStats")
  @summary("getClusterStats")
  getClusterStats(@path clusterName: string): ClusterStats;
}

model Cluster {
  name: string;
  defaultCluster?: boolean;
  status: ServerStatus;
  lastError?: MetricsCollectionError;
  brokerCount?: int32;
  onlinePartitionCount?: int32;
  topicCount?: int32;
  bytesInPerSec?: float64;
  bytesOutPerSec?: float64;
  readOnly?: boolean;
  version?: string;
  features?: ClusterFeature[];
  controller?: ControllerType;
}

alias ClusterFeature =
  "SCHEMA_REGISTRY"
  | "KAFKA_CONNECT"
  | "KSQL_DB"
  | "TOPIC_DELETION"
  | "KAFKA_ACL_VIEW"
  | "KAFKA_ACL_EDIT"
  | "CLIENT_QUOTA_MANAGEMENT"
  | "GRAPHS_ENABLED"
  | "FTS_ENABLED"
  | "FTS_DEFAULT_ENABLED";

enum ServerStatus {
  ONLINE,
  OFFLINE,
  INITIALIZING,
}

model ClusterMetrics {
  items: Metric[];
}

model ClusterStats {
  brokerCount?: int32;
  #deprecated "Unused"
  zooKeeperStatus?: int32;

  @doc("Id of broker which is cluster's controller. null, if controller not known yet.")
  activeControllers?: int32;

  onlinePartitionCount?: int32;
  offlinePartitionCount?: int32;
  inSyncReplicasCount?: int32;
  outOfSyncReplicasCount?: int32;
  underReplicatedPartitionCount?: int32;
  diskUsage?: BrokerDiskUsage[];
  version?: string;
}

model MetricsCollectionError {
  message?: string;
  stackTrace?: string;
}


model BrokerDiskUsage {
  brokerId: int32;
  segmentSize?: int64;
  segmentCount?: int32;
}

enum ControllerType {
    ZOOKEEPER,
    KRAFT
}
