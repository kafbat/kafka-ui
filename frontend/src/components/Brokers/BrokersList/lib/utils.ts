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
  onlinePartitionCount,
  offlinePartitionCount,
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
      partitionsSkew: broker.partitionsSkew,
      leadersSkew: broker.leadersSkew,
      inSyncPartitions: broker.inSyncPartitions,
      onlinePartitionCount,
      offlinePartitionCount,
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
    columnHelper.accessor('partitionsSkew', {
      header: SkewHeader,
      cell: Cell.Skew,
    }),
    columnHelper.accessor('partitionsLeader', { header: 'Leaders' }),
    columnHelper.accessor('leadersSkew', {
      header: 'Leader skew',
      cell: Cell.Skew,
    }),
    columnHelper.accessor('onlinePartitionCount', {
      header: 'Online partitions',
      cell: Cell.OnlinePartitions,
    }),
    columnHelper.accessor('port', { header: 'Port' }),
    columnHelper.accessor('host', { header: 'Host' }),
  ];
};
