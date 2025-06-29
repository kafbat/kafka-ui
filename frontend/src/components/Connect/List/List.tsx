import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import { FullConnectorInfo } from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import { useQueryPersister } from 'components/common/NewTable/Filter';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';

const kafkaConnectColumns: ColumnDef<FullConnectorInfo>[] = [
  {
    header: 'Name',
    accessorKey: 'name',
  },
  {
    header: 'Connect',
    accessorKey: 'connect',
    cell: BreakableTextCell,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
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
  },
  {
    header: 'Topics',
    accessorKey: 'topics',
    cell: TopicsCell,
    enableColumnFilter: true,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
    enableSorting: false,
  },
  {
    header: 'Status',
    accessorKey: 'status.state',
    cell: TagCell,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'arrIncludesSome',
  },
  { header: 'Running Tasks', cell: RunningTasksCell },
  { header: '', id: 'action', cell: ActionsCell },
];

const List: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { data: connectors } = useConnectors(
    clusterName,
    searchParams.get('q') || ''
  );

  const persister = useQueryPersister(kafkaConnectColumns);

  return (
    <Table
      data={connectors || []}
      columns={kafkaConnectColumns}
      enableSorting
      onRowClick={({ original: { connect, name } }) =>
        navigate(clusterConnectConnectorPath(clusterName, connect, name))
      }
      emptyMessage="No connectors found"
      setRowId={(originalRow) => `${originalRow.name}-${originalRow.connect}`}
      filterPersister={persister}
    />
  );
};

export default List;
