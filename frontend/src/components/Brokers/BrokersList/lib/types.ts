export type BrokersTableRow = {
  brokerId: number;
  size: number | undefined;
  count: number | undefined;
  port: number | undefined;
  host: string | undefined;
  partitionsLeader: number | undefined;
  partitionsSkew: number | undefined;
  leadersSkew: number | undefined;
  partitions: number | undefined;
  onlinePartitionCount: number | undefined;
  offlinePartitionCount: number | undefined;
  activeControllers: number | undefined;
};
