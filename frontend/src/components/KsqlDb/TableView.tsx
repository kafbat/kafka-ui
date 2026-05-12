import React from 'react';
import { KsqlStreamDescription, KsqlTableDescription } from 'generated-sources';
import Table from 'components/common/NewTable';
import { ColumnDef } from '@tanstack/react-table';
import BreakableTextCell from 'components/common/NewTable/BreakableTextCell';

interface TableViewProps {
  fetching: boolean;
  rows: KsqlTableDescription[] | KsqlStreamDescription[];
}

const TableView: React.FC<TableViewProps> = ({ fetching, rows }) => {
  const columns = React.useMemo<
    ColumnDef<KsqlTableDescription | KsqlStreamDescription>[]
  >(
    () => [
      { header: 'Name', accessorKey: 'name', cell: BreakableTextCell },
      { header: 'Topic', accessorKey: 'topic', cell: BreakableTextCell },
      { header: 'Key Format', accessorKey: 'keyFormat' },
      { header: 'Value Format', accessorKey: 'valueFormat' },
      {
        header: 'Is Windowed',
        accessorKey: 'isWindowed',
        cell: ({ row }) =>
          'isWindowed' in row.original ? String(row.original.isWindowed) : '-',
      },
    ],
    []
  );
  return (
    <Table
      data={rows || []}
      columns={columns}
      emptyMessage={fetching ? 'Loading...' : 'No rows found'}
      enableSorting
    />
  );
};

export default TableView;
