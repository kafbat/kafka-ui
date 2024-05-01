import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import { FullConnectorInfo } from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useSearchParams } from 'react-router-dom';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';
import BatchActionsBar from './BatchActionsBar';
import { ConnectorTitleCell } from './ConnectorTitleCell';

const List: React.FC = () => {
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const [searchParams] = useSearchParams();
  const { data: connectors } = useConnectors(
    clusterName,
    searchParams.get('q') || ''
  );

  const columns = React.useMemo<ColumnDef<FullConnectorInfo>[]>(
    () => [
      { header: 'Name', accessorKey: 'name', cell: ConnectorTitleCell },
      { header: 'Connect', accessorKey: 'connect' },
      { header: 'Type', accessorKey: 'type' },
      { header: 'Plugin', accessorKey: 'connectorClass' },
      { header: 'Topics', cell: TopicsCell },
      { header: 'Status', accessorKey: 'status.state', cell: TagCell },
      { header: 'Running Tasks', cell: RunningTasksCell },
      { header: '', id: 'action', cell: ActionsCell },
    ],
    []
  );

  return (
    <Table
      data={connectors || []}
      columns={columns}
      enableSorting
      emptyMessage="No connectors found"
      setRowId={(originalRow) => `${originalRow.name}-${originalRow.connect}`}
      enableRowSelection
      batchActionsBar={BatchActionsBar}
    />
  );
};

export default List;
