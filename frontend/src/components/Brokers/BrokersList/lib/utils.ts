import { Broker, BrokerDiskUsage } from 'generated-sources';
import * as Cell from 'components/Brokers/BrokersList/TableCells/TableCells';
import { createColumnHelper } from '@tanstack/react-table';
import { keyBy } from 'lib/functions/keyBy';
import SkewHeader from 'components/Brokers/BrokersList/SkewHeader/SkewHeader';

import { BrokersTableRow } from './types';
import { NA_DISK_USAGE } from './constants';

type GetBrokersTableRowsParams = {
  brokers: Broker[] | undefined;
  diskUsage: BrokerDiskUsage[] | undefined;
  activeControllers: number | undefined;
  onlinePartitionCount: number | undefined;
  offlinePartitionCount: number | undefined;
};

export const getBrokersTableRows = ({
  brokers = [],
  diskUsage = [],
  activeControllers,
}: GetBrokersTableRowsParams): BrokersTableRow[] => {
  if (!brokers || brokers.length === 0) {
    return [];
  }

  const diskUsageByBroker = keyBy(diskUsage, 'brokerId');

  return brokers.map((broker) => {
    const diskUse = diskUsageByBroker[broker.id] || NA_DISK_USAGE;

    return {
      brokerId: broker.id,
      size: diskUse.segmentSize,
      count: diskUse.segmentCount,
      port: broker.port,
      host: broker.host,
      partitionsLeader: broker.partitionsLeader,
      leadersSkew: broker.leadersSkew,
      replicas: broker.partitions,
      inSyncReplicas: broker.inSyncPartitions,
      replicasSkew: broker.partitionsSkew,
      activeControllers,
    };
  });
};

export const getBrokersTableColumns = () => {
  const columnHelper = createColumnHelper<BrokersTableRow>();

  return [
    columnHelper.accessor('brokerId', {
      header: 'Broker ID',
      cell: Cell.BrokerId,
    }),
    columnHelper.accessor('size', {
      header: 'Disk usage',
      cell: Cell.DiscUsage,
    }),
    columnHelper.accessor('inSyncReplicas', {
      header: 'In Sync Replicas',
      cell: Cell.InSyncReplicas,
    }),
    columnHelper.accessor('replicas', {
      header: 'Replicas',
      cell: Cell.Replicas,
    }),
    columnHelper.accessor('replicasSkew', {
      header: SkewHeader,
      cell: Cell.Skew,
    }),
    columnHelper.accessor('partitionsLeader', { header: 'Leaders' }),
    columnHelper.accessor('leadersSkew', {
      header: 'Leaders skew',
      cell: Cell.Skew,
    }),
    columnHelper.accessor('port', { header: 'Port' }),
    columnHelper.accessor('host', { header: 'Host' }),
  ];
};
