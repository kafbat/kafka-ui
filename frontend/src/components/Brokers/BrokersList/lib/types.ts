export type BrokersTableRow = {
  brokerId: number;
  size: string | number;
  count: string | number;
  port: number | undefined;
  host: string | undefined;
  partitionsLeader: number | undefined;
  partitionsSkew: number | undefined;
  leadersSkew: number | undefined;
  inSyncPartitions: number;
  activeControllers: number | undefined;
};
