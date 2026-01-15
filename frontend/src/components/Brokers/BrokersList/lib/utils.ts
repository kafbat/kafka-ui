import { Broker, BrokerDiskUsage } from 'generated-sources';
import * as Cell from 'components/Brokers/BrokersList/TableCells/TableCells';
import { createColumnHelper } from '@tanstack/react-table';
import { keyBy } from 'lib/functions/keyBy';
import SkewHeader from 'components/Brokers/BrokersList/SkewHeader/SkewHeader';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { formatBytes } from 'components/common/BytesFormatted/utils';

import { BrokersTableRow } from './types';
import { NA, NA_DISK_USAGE } from './constants';

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
      meta: {
        csvFn: (row) => {
          const isActive = row.activeControllers === row.brokerId;

          return `${row.brokerId} ${isActive ? '(Active)' : ''}`;
        },
      },
    }),
    columnHelper.accessor('size', {
      header: 'Disk usage',
      cell: Cell.DiscUsage,
      meta: {
        csvFn: (row) => {
          if (row.size === undefined) return NA;

          return `${formatBytes(row.size, 2)}, ${row.count} segment(s)`;
        },
      },
    }),
    columnHelper.accessor('inSyncReplicas', {
      header: 'In Sync Replicas',
      cell: Cell.InSyncReplicas,
      meta: { csvFn: (row) => String(row.inSyncReplicas) },
    }),
    columnHelper.accessor('replicas', {
      header: 'Replicas',
      cell: Cell.Replicas,
      meta: { csvFn: (row) => row.replicas?.toString() || '' },
    }),
    columnHelper.accessor('replicasSkew', {
      header: SkewHeader,
      meta: {
        csv: 'Replicas skew',
        csvFn: (row) => Cell.getSkewValue(row.replicasSkew),
      },
      cell: Cell.Skew,
    }),
    columnHelper.accessor('partitionsLeader', { header: 'Leaders' }),
    columnHelper.accessor('leadersSkew', {
      header: 'Leaders skew',
      cell: Cell.Skew,
      meta: { csvFn: (row) => Cell.getSkewValue(row.leadersSkew) },
    }),
    columnHelper.accessor('port', { header: 'Port' }),
    columnHelper.accessor('host', { header: 'Host', cell: BreakableTextCell }),
  ];
};
