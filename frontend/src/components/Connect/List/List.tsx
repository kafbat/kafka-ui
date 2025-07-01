import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import { FullConnectorInfo } from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';

const kafkaConnectColumns: ColumnDef<FullConnectorInfo>[] = [
  {
    header: 'Name',
    accessorKey: 'name',
    cell: BreakableTextCell,
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
  const navigate = useNavigate();
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { data: connectors } = useConnectors(
    clusterName,
    searchParams.get('q') || ''
  );

  const filterPersister = useQueryPersister(kafkaConnectColumns);

  return (
    <Table
      data={connectors || []}
      columns={kafkaConnectColumns}
      enableSorting
      enableColumnResizing
      tableName="KafkaConnect"
      onRowClick={({ original: { connect, name } }) =>
        navigate(clusterConnectConnectorPath(clusterName, connect, name))
      }
      emptyMessage="No connectors found"
      setRowId={(originalRow) => `${originalRow.name}-${originalRow.connect}`}
      filterPersister={filterPersister}
    />
  );
};

export default List;
