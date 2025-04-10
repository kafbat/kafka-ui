export type BrokersTableRow = {
  brokerId: number;
  size: number | undefined;
  count: number | undefined;
  port: number | undefined;
  host: string | undefined;
  partitionsLeader: number | undefined;
  leadersSkew: number | undefined;
  replicas: number | undefined;
  inSyncReplicas: number | undefined;
  replicasSkew: number | undefined;
  activeControllers: number | undefined;
};
