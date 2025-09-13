import React from 'react';
import { SortOrder, Topic, TopicColumnsToSort } from 'generated-sources';
import { ColumnDef } from '@tanstack/react-table';
import Table, { SizeCell } from 'components/common/NewTable';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterName } from 'lib/interfaces/cluster';
import { useSearchParams } from 'react-router-dom';
import ClusterContext from 'components/contexts/ClusterContext';
import { useTopics } from 'lib/hooks/api/topics';
import { PER_PAGE } from 'lib/constants';

import { TopicTitleCell } from './TopicTitleCell';
import ActionsCell from './ActionsCell';
import BatchActionsbar from './BatchActionsBar';

const TopicTable: React.FC = () => {
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();
  const [searchParams] = useSearchParams();
  const { isReadOnly } = React.useContext(ClusterContext);
  const { data } = useTopics({
    clusterName,
    page: Number(searchParams.get('page') || 1),
    perPage: Number(searchParams.get('perPage') || PER_PAGE),
    search: searchParams.get('q') || undefined,
    showInternal: !searchParams.has('hideInternal'),
    orderBy: (searchParams.get('sortBy') as TopicColumnsToSort) || undefined,
    sortOrder:
      (searchParams.get('sortDirection')?.toUpperCase() as SortOrder) ||
      undefined,
  });

  const topics = data?.topics || [];
  const pageCount = data?.pageCount || 0;

  const columns = React.useMemo<ColumnDef<Topic>[]>(
    () => [
      {
        id: TopicColumnsToSort.NAME,
        header: 'Topic Name',
        accessorKey: 'name',
        cell: TopicTitleCell,
        meta: {
          width: '100%',
        },
      },
      {
        id: TopicColumnsToSort.TOTAL_PARTITIONS,
        header: 'Partitions',
        accessorKey: 'partitionCount',
        size: 100,
      },
      {
        id: TopicColumnsToSort.OUT_OF_SYNC_REPLICAS,
        header: 'Out of sync replicas',
        accessorKey: 'partitions',
        size: 154,
        cell: ({ getValue }) => {
          const partitions = getValue<Topic['partitions']>();
          if (partitions === undefined || partitions.length === 0) {
            return 0;
          }
          return partitions.reduce((memo, { replicas }) => {
            const outOfSync = replicas?.filter(({ inSync }) => !inSync);
            return memo + (outOfSync?.length || 0);
          }, 0);
        },
      },
      {
        header: 'Replication Factor',
        accessorKey: 'replicationFactor',
        enableSorting: false,
        size: 148,
      },
      {
        id: TopicColumnsToSort.MESSAGES_COUNT,
        header: 'Number of messages',
        accessorKey: 'messageCount',
        size: 146,
      },
      {
        id: TopicColumnsToSort.SIZE,
        header: 'Size',
        accessorKey: 'segmentSize',
        size: 100,
        cell: SizeCell,
      },
      {
        id: 'actions',
        header: '',
        cell: ActionsCell,
        size: 60,
      },
    ],
    []
  );

  return (
    <Table
      data={topics}
      pageCount={pageCount}
      columns={columns}
      enableSorting
      serverSideProcessing
      batchActionsBar={BatchActionsbar}
      enableRowSelection={
        !isReadOnly ? (row) => !row.original.internal : undefined
      }
      emptyMessage="No topics found"
    />
  );
};

export default TopicTable;
