import { Broker, BrokerDiskUsage } from 'generated-sources';
import * as Cell from 'components/Brokers/BrokersList/TableCells/TableCells';
import SkewHeader from 'components/Brokers/BrokersList/SkewHeader/SkewHeader';
import { createColumnHelper } from '@tanstack/react-table';

import { BrokersTableRow } from './types';
import { NA } from './constants';

const brokerResourceMapper = (broker: Broker) => ({
  brokerId: broker.id,
  segmentSize: NA,
  segmentCount: NA,
});

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
  const brokersResource = diskUsage.length
    ? diskUsage
    : brokers.map(brokerResourceMapper);

  return brokersResource.map(({ brokerId, segmentSize, segmentCount }) => {
    const broker = brokers?.find(({ id }) => id === brokerId);

    return {
      brokerId,
      size: segmentSize || NA,
      count: segmentCount || NA,
      port: broker?.port,
      host: broker?.host,
      partitionsLeader: broker?.partitionsLeader,
      partitionsSkew: broker?.partitionsSkew,
      leadersSkew: broker?.leadersSkew,
      onlinePartitionCount: onlinePartitionCount ?? 0,
      offlinePartitionCount: offlinePartitionCount ?? 0,
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
