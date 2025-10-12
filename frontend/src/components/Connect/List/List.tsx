import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import { FullConnectorInfo } from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';
import { useLocalStoragePersister } from 'components/common/NewTable/ColumnResizer/lib';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import useFts from 'components/common/Fts/useFts';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';
import { KafkaConnectLinkCell } from './KafkaConnectLinkCell';

const kafkaConnectColumns: ColumnDef<FullConnectorInfo, string>[] = [
  {
    header: 'Name',
    accessorKey: 'name',
    cell: KafkaConnectLinkCell,
    enableResizing: true,
  },
  {
    header: 'Connect',
    accessorKey: 'connect',
    cell: BreakableTextCell,
    filterFn: 'arrIncludesSome',
    meta: {
      filterVariant: 'multi-select',
    },
    enableResizing: true,
  },
  {
    header: 'Type',
    accessorKey: 'type',
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    size: 120,
  },
  {
    header: 'Plugin',
    accessorKey: 'connectorClass',
    cell: BreakableTextCell,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    enableResizing: true,
  },
  {
    header: 'Topics',
    accessorKey: 'topics',
    cell: TopicsCell,
    enableColumnFilter: true,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    enableResizing: true,
  },
  {
    header: 'Status',
    accessorKey: 'status.state',
    cell: TagCell,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
  },
  {
    id: 'running_task',
    header: 'Running Tasks',
    cell: RunningTasksCell,
    size: 120,
  },
  {
    header: '',
    id: 'action',
    cell: ActionsCell,
    size: 60,
  },
];

const List: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { isFtsEnabled } = useFts('connects');
  const { data: connectors } = useConnectors(
    clusterName,
    searchParams.get('q') || '',
    isFtsEnabled
  );

  const filterPersister = useQueryPersister(kafkaConnectColumns);
  const columnSizingPersister = useLocalStoragePersister('KafkaConnect');

  return (
    <Table
      data={connectors || []}
      columns={kafkaConnectColumns}
      enableSorting
      enableColumnResizing
      columnSizingPersister={columnSizingPersister}
      emptyMessage="No connectors found"
      setRowId={(originalRow) => `${originalRow.name}-${originalRow.connect}`}
      filterPersister={filterPersister}
    />
  );
};

export default List;
