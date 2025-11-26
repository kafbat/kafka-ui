import React from 'react';
import { Connect } from 'generated-sources';
import Table from 'components/common/NewTable';
import useAppParams from 'lib/hooks/useAppParams';
import { ClusterName } from 'lib/interfaces/cluster';
import { useNavigate } from 'react-router-dom';
import { clusterConnectorsPath } from 'lib/paths';
import { ColumnDef, createColumnHelper } from '@tanstack/react-table';
import { useQueryPersister } from 'components/common/NewTable/ColumnFilter';

import ConnectorsCell from './Cells/ConnectorsCell';
import NameCell from './Cells/NameCell';
import TasksCell from './Cells/TasksCell';

const helper = createColumnHelper<Connect>();
export const columns = [
  helper.accessor('name', {
    cell: NameCell,
    size: 600,
    meta: { filterVariant: 'text' },
    filterFn: 'includesString',
  }),
  helper.accessor('version', {
    header: 'Version',
    cell: ({ getValue }) => getValue(),
    enableSorting: true,
    meta: { filterVariant: 'multi-select' },
    filterFn: 'includesSome',
  }),
  helper.display({
    header: 'Connectors',
    id: 'connectors',
    cell: (props) => <ConnectorsCell connect={props.row.original} />,
    size: 100,
  }),
  helper.display({
    header: 'Running tasks',
    id: 'tasks',
    cell: (props) => <TasksCell connect={props.row.original} />,
    size: 100,
  }),
];

interface Props {
  connects: Connect[];
}
const List = ({ connects }: Props) => {
  const navigate = useNavigate();
  const { clusterName } = useAppParams<{ clusterName: ClusterName }>();

  const filterPersister = useQueryPersister(columns as ColumnDef<Connect>[]);
  return (
    <Table
      data={connects}
      columns={columns}
      onRowClick={({ original: { name } }) => {
        navigate(`${clusterConnectorsPath(clusterName)}?connect=${name}`);
      }}
      emptyMessage="No kafka connect clusters"
      enableSorting
      filterPersister={filterPersister}
    />
  );
};

export default List;
