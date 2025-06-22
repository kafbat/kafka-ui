import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import { FullConnectorInfo } from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useNavigate, useSearchParams } from 'react-router-dom';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';
import useQueryParamsPersister from 'components/common/NewTable/FIlter/Persister';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';

const List: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { data: connectors } = useConnectors(
    clusterName,
    searchParams.get('q') || ''
  );

  const columns = React.useMemo<ColumnDef<FullConnectorInfo>[]>(
    () => [
      { header: 'Name', accessorKey: 'name', cell: BreakableTextCell },
      {
        header: 'Connect',
        accessorKey: 'connect',
        cell: BreakableTextCell,
        meta: { filterVariant: 'multi-select' },
        filterFn: 'includesSome',
      },
      {
        header: 'Type',
        accessorKey: 'type',
        meta: { filterVariant: 'multi-select' },
        filterFn: 'includesSome',
      },
      {
        header: 'Plugin',
        accessorKey: 'connectorClass',
        cell: BreakableTextCell,
        meta: { filterVariant: 'multi-select' },
        filterFn: 'includesSome',
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
        filterFn: 'includesSome',
      },
      { header: 'Running Tasks', cell: RunningTasksCell },
      { header: '', id: 'action', cell: ActionsCell },
    ],
    []
  );

  const persister = useQueryParamsPersister(columns);

  return (
    <Table
      data={connectors || []}
      columns={columns}
      enableSorting
      onRowClick={({ original: { connect, name } }) =>
        navigate(clusterConnectConnectorPath(clusterName, connect, name))
      }
      emptyMessage="No connectors found"
      setRowId={(originalRow) => `${originalRow.name}-${originalRow.connect}`}
      persister={persister}
    />
  );
};

export default List;
