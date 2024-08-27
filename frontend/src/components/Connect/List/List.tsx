import React from 'react';
import useAppParams from 'lib/hooks/useAppParams';
import { clusterConnectConnectorPath, ClusterNameRoute } from 'lib/paths';
import Table, { TagCell } from 'components/common/NewTable';
import {
  ConnectorState,
  ConnectorType,
  FullConnectorInfo,
} from 'generated-sources';
import { useConnectors } from 'lib/hooks/api/kafkaConnect';
import { ColumnDef } from '@tanstack/react-table';
import { useNavigate } from 'react-router-dom';

import ActionsCell from './ActionsCell';
import TopicsCell from './TopicsCell';
import RunningTasksCell from './RunningTasksCell';

const List: React.FC = () => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<ClusterNameRoute>();
  const { data: connectors } = useConnectors(clusterName);

  const columns = React.useMemo<ColumnDef<FullConnectorInfo>[]>(
    () => [
      { header: 'Name', accessorKey: 'name' },
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

  const connectorTypeOptions = Object.values(ConnectorType).map((state) => ({
    label: state,
    value: state,
  }));

  const connectorStateOptions = Object.values(ConnectorState).map((state) => ({
    label: state,
    value: state,
  }));

  const columnSearchPlaceholders: {
    id: string;
    columnName: string;
    placeholder: string;
    type: string;
    options?: { label: string; value: string }[];
  }[] = [
    {
      id: 'name',
      columnName: 'name',
      placeholder: 'Search by Name',
      type: 'input',
    },
    {
      id: 'connect',
      columnName: 'connect',
      placeholder: 'Search by Connect',
      type: 'input',
    },
    {
      id: 'type',
      columnName: 'type',
      placeholder: 'Select Type',
      type: 'autocomplete',
      options: connectorTypeOptions,
    },
    {
      id: 'connectorClass',
      columnName: 'connectorClass',
      placeholder: 'Search by Plugin',
      type: 'input',
    },
    {
      id: 'Topics',
      columnName: 'topics',
      placeholder: 'Search by Topics',
      type: 'multiInput',
    },
    {
      id: 'status_state',
      columnName: 'status',
      placeholder: 'Select Status',
      type: 'autocomplete',
      options: connectorStateOptions,
    },
  ];

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
      enableColumnSearch
      columnSearchPlaceholders={columnSearchPlaceholders}
    />
  );
};

export default List;
